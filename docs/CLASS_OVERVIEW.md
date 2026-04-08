# Class Overview

This document explains how classes are organized and how they interact in the
current codebase.

## Package structure

- `app`:
  - Startup wiring and module initialization.
- `model`:
  - Core domain records/enums/exceptions shared across modules.
- `storage`:
  - Tenant-safe path and storage abstractions.
- `sync`:
  - Event ingestion path, watcher integration, queue/pipeline, reconciliation.
- `metadata`:
  - Directory metadata profiling, fingerprinting, and versioned registry.
- `format`:
  - Format plugin contracts and parse exceptions.
- `transform`:
  - Canonical transformation contracts and exceptions.

## Core classes by area

## 1) Ingestion contract and validation (`model`, `storage`, `sync`)

- `IngestionContract`:
  - Normalized event contract:
    `tenantId`, `path`, `operation`, `etagOrVersion`, `timestamp`, trace fields.
- `IngestionOperation`:
  - `ADD`, `EDIT`, `DELETE`.
- `IngestionContractValidator`:
  - Validates required fields and normalizes tenant/path.
- `IngestionValidationException`:
  - Typed validation/runtime error for ingestion safety.
- `TenantPathPolicy`:
  - Tenant ID rules and relative-path traversal checks.
- `TenantPathResolver`:
  - Resolves tenant-scoped source/canonical filesystem paths.
- `EventOrderingResolver`:
  - Deterministic timestamp-based ordering comparator.

## 2) Near-real-time sync path (`sync`)

- `RawWatcherEvent`:
  - Raw watcher event model (`CREATE`, `MODIFY`, `DELETE`).
- `FileWatcherAdapter`:
  - Watcher lifecycle contract (`start`, `stop`, `isRunning`).
- `NioFileWatcherAdapter`:
  - `WatchService` implementation that listens to source roots.
- `WatcherEventMapper`:
  - Converts `RawWatcherEvent` to validated `IngestionContract`.
- `EventIngressQueue`:
  - Bounded queue for ingestion events.
- `EventPipeline`:
  - Accepts validated events and records processed state.
- `ProcessedEventStore`:
  - Processed-state abstraction for restart continuity.
- `FileProcessedEventStore`:
  - Properties-file implementation for local processed state.

## 3) Reconciliation and drift closure (`sync`)

- `SourceSnapshot`:
  - Captured file state for a tenant/source root.
- `SourceSnapshotStore`:
  - Snapshot persistence contract.
- `FileSourceSnapshotStore`:
  - Local file-backed snapshot store.
- `DriftDetector` + `DriftResult`:
  - Compares snapshots and classifies created/modified/deleted/unchanged paths.
- `ReconciliationScanner`:
  - Runs periodic scan, detects drift, emits corrective ingestion events.
- `ReconciliationRunResult`:
  - Run summary counters.
- `ReconciliationMetrics`:
  - In-memory run counters.
- `ReconciliationScheduler`:
  - Fixed-delay scheduler for reconciliation runs.

## 4) Metadata profiling and registry (`metadata`, `model`)

- `FingerprintCalculator`:
  - Computes deterministic structural fingerprint (`SHA-256`).
- `FormatProfiler`:
  - Builds `DirectoryMetadataProfile` from metadata + structural keys.
- `DirectoryMetadataProfile`:
  - Versioned metadata profile per tenant directory.
- `FormatFingerprint`:
  - Fingerprint value + algorithm descriptor.
- `MetadataVersionRecord`:
  - Version-history record for metadata changes.
- `MetadataRegistry`:
  - Registry contract (`upsertProfile`, `getLatest`, `history`).
- `FileMetadataRegistry`:
  - Local file-backed latest+history metadata store.

## 5) App wiring (`app`)

- `Application`:
  - Initializes:
    - contract validation + tenant path safety,
    - sync pipeline dependencies,
    - reconciliation dependencies,
    - metadata registry/profiler dependencies.
  - Currently initializes components; runtime orchestration is still iterative.

## Runtime flow (current)

1. Source changes are observed by watcher (`NioFileWatcherAdapter`).
2. Raw events are mapped (`WatcherEventMapper`) to `IngestionContract`.
3. Contract is validated (`IngestionContractValidator`) and queued (`EventIngressQueue`).
4. Pipeline (`EventPipeline`) drains and marks processed state.
5. Reconciliation (`ReconciliationScanner`) periodically scans source roots,
   detects drift (`DriftDetector`), and re-emits corrective events through the
   same pipeline.
6. Metadata profiling (`FormatProfiler`) and registry (`FileMetadataRegistry`)
   persist directory-level format metadata and version history.

## Design rules followed by classes

- Tenant isolation is enforced at path and registry boundaries.
- Validation happens before processing.
- Errors are explicit and typed; no silent exception swallowing.
- Local persistence is used for state continuity (processed events, snapshots,
  metadata profiles/history).
