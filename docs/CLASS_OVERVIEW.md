# Class Overview

This document lists current class definitions and runtime interactions in
`com.acme.docsync`.

## Package structure

- `app`: startup wiring and dependency assembly.
- `model`: domain records, enums, and typed exceptions.
- `storage`: file-backed stores and tenant/path safety primitives.
- `sync`: watcher ingress, queue/pipeline, reconciliation, runtime health.
- `metadata`: directory profiling, fingerprinting, profile registry.
- `format`: plugin interfaces, detection contracts, parse errors.
- `transform`: canonical transform chain and publish/delete coordinators.

## 1) Ingestion and path safety (`model`, `storage`, `sync`)

- `IngestionContract`: normalized event envelope for ingestion.
- `IngestionOperation`: operation enum (`ADD`, `EDIT`, `DELETE`).
- `IngestionContractValidator`: normalization + required-field validation.
- `IngestionValidationException`: typed validation failure.
- `TenantPathPolicy`: tenant ID and path traversal guardrails.
- `TenantPathResolver`: tenant-scoped root/path resolver.
- `EventOrderingResolver`: deterministic ordering comparator.
- `DocumentEvent`: file-level change event used by detector abstractions.
- `ChangeDetector`: polling contract that returns `Stream<DocumentEvent>`.
- `TenantStorage`: tenant-isolated listing contract.

## 2) Watcher ingress and processing (`sync`)

- `RawWatcherEvent`: raw watcher event model (`CREATE`, `MODIFY`, `DELETE`).
- `FileWatcherAdapter`: watcher lifecycle interface.
- `NioFileWatcherAdapter`: `WatchService` implementation.
- `WatcherEventMapper`: maps raw watcher events to `IngestionContract`.
- `EventIngressQueue`: bounded blocking queue for ingress pressure control.
- `EventPipeline`: validates, orders, enqueues, and marks processed events.
- `ProcessedEventStore`: processed marker contract.
- `FileProcessedEventStore`: file-backed processed marker implementation.

## 3) Reconciliation and health (`sync`)

- `SourceSnapshot`: captured source file state.
- `SourceSnapshotStore`: snapshot persistence contract.
- `FileSourceSnapshotStore`: file-backed snapshot store.
- `DriftDetector` + `DriftResult`: computes created/modified/deleted deltas.
- `ReconciliationScanner`: drift scan + corrective event emission.
- `ReconciliationRunResult`: per-run counters.
- `ReconciliationMetrics`: aggregate reconciliation counters.
- `ReconciliationScheduler`: fixed-delay scanner runner.
- `RuntimeHealthService`: builds runtime health snapshots.
- `RuntimeHealthReport`: watcher/scheduler/queue/failed-source health record.

## 4) Metadata profiling (`metadata`, `model`)

- `FingerprintCalculator`: deterministic metadata fingerprint (`SHA-256`).
- `FormatProfiler`: builds `DirectoryMetadataProfile`.
- `MetadataRegistry`: profile/history contract.
- `FileMetadataRegistry`: file-backed metadata registry.
- `DirectoryMetadataProfile`: current profile record.
- `FormatFingerprint`: algorithm + fingerprint value.
- `MetadataVersionRecord`: metadata history record.
- `DirectoryMetadata`: source directory metadata model.

## 5) Format and transform pipeline (`format`, `transform`, `model`)

- `FormatPlugin`: plugin contract (`detect`, `parse`, `map`, `validate`).
- `FormatPluginRegistry`: plugin registration and best-match resolution.
- `FormatDetectionResult`: plugin support/score result model.
- `FormatParseException`: parse-stage error.
- `CanonicalTransformer`: transform interface for source to canonical.
- `PluginCanonicalTransformer`: plugin-backed transform implementation.
- `CanonicalOutputValidator`: canonical output invariant checks.
- `CanonicalValidationResult`: canonical validation result model.
- `TransformException`: transform-stage error.
- `SourceDocument` -> `ParsedDocument` -> `CanonicalDocument`: transform model
  chain.

## 6) Publish/delete and parity checks (`transform`, `storage`, `model`)

- `PublishCoordinator`: persists canonical output and publish state.
- `DeleteCoordinator`: writes tombstone, applies canonical delete, marks index
  deleted.
- `CanonicalDocumentStore` + `FileCanonicalDocumentStore`: canonical store
  contract + implementation.
- `IndexPublishStateStore` + `FileIndexPublishStateStore`: publish-state
  contract + implementation.
- `TombstoneStore` + `FileTombstoneStore`: tombstone contract + implementation.
- `ProvenanceStore` + `FileProvenanceStore`: source/canonical linkage contract
  + implementation.
- `ConsistencyChecker` + `ConsistencyReport`: parity checks across canonical,
  index, and tombstone states.
- `AtomicFileWriter`: atomic file writes used by file-backed stores.
- `PublishStateRecord`, `TombstoneRecord`, `ProvenanceRecord`, `DeletePolicy`:
  persistence domain records.

## 7) Application wiring (`app`)

- `Application` composes all major components:
  - ingestion validator, path policy/resolver, queue, pipeline;
  - reconciliation scanner/scheduler/metrics;
  - metadata profiler and registry;
  - plugin registry and canonical transformer;
  - provenance/canonical/index/tombstone stores;
  - publish/delete coordinators;
  - runtime health and consistency checker.

## Runtime interactions (current)

1. Filesystem changes are observed (`NioFileWatcherAdapter`) as
   `RawWatcherEvent`.
2. `WatcherEventMapper` converts raw events to `IngestionContract`.
3. `EventPipeline.accept()` validates and enqueues to `EventIngressQueue`.
4. `EventPipeline.drainAndMarkProcessed()` drains and marks processed state.
5. Reconciliation runs periodically:
   `ReconciliationScheduler` -> `ReconciliationScanner` -> `DriftDetector`.
6. Drift emits corrective ingestion events back through the same pipeline.
7. Metadata path computes directory profiles:
   `FingerprintCalculator` + `FormatProfiler` -> `MetadataRegistry`.
8. Canonicalization path resolves plugins and transforms:
   `FormatPluginRegistry` + `PluginCanonicalTransformer`.
9. Publish/delete path updates canonical/index/tombstone stores via
   `PublishCoordinator` and `DeleteCoordinator`.
10. Operational visibility is provided by `RuntimeHealthService` and
    `ConsistencyChecker`.

## Design constraints reflected in classes

- Tenant/path validation is applied before persistence.
- File-backed stores keep runtime state restart-safe.
- Transformation and publish/delete concerns are isolated by contracts.
- Reconciliation reuses the same ingestion pipeline for consistency.
- Health and consistency are explicit services.
