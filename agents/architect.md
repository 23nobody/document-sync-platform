# Architect Sub-Agent

## Purpose

Design safe technical approaches aligned with module boundaries.

## Inputs

- Plan from `Planner`
- Existing module/package structure
- PRD requirements and non-functional constraints

## Responsibilities

1. Propose architecture and API contract changes (if needed).
2. Preserve separation across `storage`, `metadata`, `sync`, `format`,
   `transform`, `model`, `app`.
3. Ensure backward compatibility unless explicitly waived.
4. Define invariants for reconciliation and canonical validation.

## Required Output Format

- Design summary
- Affected modules/files
- Contract/interface changes
- Invariants to preserve
- Migration/rollback notes

## Safety Constraints

- No design may permit tenant data leakage.
- No design may skip provenance or validator updates when schema changes.
- Must include failure-mode handling strategy.

## PR1 Scope Note

- Leave idempotency out of scope for PR1.
- Defer idempotency key design/implementation to a follow-up PR.
- Keep ordering and validation design reusable for future idempotency support.

## Risks and Mitigations

- Tenant boundary leakage via malformed paths:
  - Mitigation: normalize and validate resolved absolute paths before use.
- Out-of-order updates causing stale apply:
  - Mitigation: centralized ordering resolver used by all future processors.
- Inconsistent contract enforcement:
  - Mitigation: single validation entry point required before any write.
- Over-scoping PR1:
  - Mitigation: explicitly defer watcher/scanner runtime and publishing logic.

## Validation Checklist

- [ ] `tenantId` and directory naming conventions are documented and enforced.
- [ ] Ingestion contract exists with all required WS2 fields.
- [ ] Path traversal and cross-tenant writes are rejected by tests.
- [ ] Ordering utilities are deterministic by tests.
- [ ] Idempotency is explicitly deferred and not implemented in PR1.
- [ ] No watcher/scanner runtime loop is included in PR1.
- [ ] Error handling returns explicit, actionable messages.

## Current Target: PR2 design (event path only)

### Design summary

Add near-real-time ingestion via filesystem watcher adapters that emit normalized
`IngestionContract` events into a bounded in-process pipeline. PR2 introduces
event-path runtime only; periodic reconciliation scanner stays out of scope.

### Affected modules/files

- `src/main/java/com/acme/docsync/sync/`
  - `FileWatcherAdapter.java`
  - `WatcherEventMapper.java`
  - `EventIngressQueue.java`
  - `EventPipeline.java`
  - `ProcessedEventStore.java` (local state for restart safety)
- `src/main/java/com/acme/docsync/model/`
  - extend `IngestionContract` only if trace fields are needed
- `src/main/java/com/acme/docsync/app/`
  - `Application.java` startup wiring for watcher pipeline
- `src/test/java/com/acme/docsync/sync/`
  - watcher mapping and pipeline behavior tests

### Contract/interface changes

1. `FileWatcherAdapter`
   - `void start()`
   - `void stop()`
   - `boolean isRunning()`
   - Emits raw events (`CREATE`, `MODIFY`, `DELETE`) with tenant/source context.
2. `WatcherEventMapper`
   - `IngestionContract toIngestionEvent(RawWatcherEvent event, Instant now)`
   - Maps watcher kinds to `ADD`/`EDIT`/`DELETE`.
   - Applies PR1 validator before queueing.
3. `EventIngressQueue`
   - `boolean offer(IngestionContract event)`
   - `IngestionContract take()`
   - Bounded queue with backpressure strategy and explicit rejection errors.
4. `EventPipeline`
   - `void accept(IngestionContract event)`
   - Runs validation + ordering pre-checks and records processed state.
5. `ProcessedEventStore`
   - `void markProcessed(IngestionContract event)`
   - `boolean wasRecentlyProcessed(String tenantId, String path, Instant ts)`
   - Restart-safety metadata only; no dedupe/idempotency claims in PR2.

### Invariants to preserve

- Tenant isolation:
  - Watchers are registered per tenant/source root only.
  - Mapped event path must resolve through `TenantPathResolver`.
- Contract safety:
  - No event bypasses `IngestionContractValidator`.
- Determinism:
  - For same event stream order, pipeline state evolution is repeatable.
- Operational safety:
  - Queue overflow never crashes process silently; errors are explicit.

### Failure-mode handling strategy

- Watcher unavailable or root missing:
  - Surface startup error and fail fast for that watcher configuration.
- Transient filesystem notifications:
  - Map only supported event kinds; discard unsupported kinds with warning.
- Queue pressure:
  - Reject with explicit error metric/log and continue service.
- Processing failure:
  - Preserve cause chain and context (`tenantId`, `path`, event kind).

### Migration/rollback notes

- Additive changes only; no schema migrations.
- Rollback path: disable watcher wiring in `Application` and remove sync runtime
  classes without impacting PR1 contract/validation foundation.
- PR3 scanner can reuse `EventPipeline` and `ProcessedEventStore` interfaces.

## Current Target: PR3 design (periodic reconciliation scanner)

### Design summary

Add a scheduled reconciliation scanner that walks tenant source directories,
compares source snapshot to processed state, and emits corrective ingestion
events through the same `EventPipeline` used by watcher events. Goal is drift
closure for missed/late events without introducing new processing semantics.

### Affected modules/files

- `src/main/java/com/acme/docsync/sync/`
  - `ReconciliationScanner.java`
  - `ReconciliationScheduler.java`
  - `SourceSnapshotStore.java`
  - `DriftDetector.java`
  - `ReconciliationMetrics.java` (counters only)
- `src/main/java/com/acme/docsync/storage/`
  - optional source tree listing utility if existing API is insufficient
- `src/main/java/com/acme/docsync/app/`
  - `Application.java` schedule wiring and lifecycle stop hooks
- `src/test/java/com/acme/docsync/sync/`
  - scanner drift recovery tests
  - repeated-run stability tests

### Contract/interface changes

1. `SourceSnapshotStore`
   - `SourceSnapshot capture(String tenantId, String sourceId)`
   - `void persist(SourceSnapshot snapshot)`
   - Local snapshot persistence keyed by tenant/source.
2. `DriftDetector`
   - `DriftResult detect(SourceSnapshot current, SourceSnapshot previous)`
   - Outputs `created`, `modified`, `deleted`, and `unchanged` sets.
3. `ReconciliationScanner`
   - `ReconciliationRunResult runOnce()`
   - For each drift item, emits normalized `IngestionContract` into
     `EventPipeline.accept(...)`.
4. `ReconciliationScheduler`
   - `void start()`
   - `void stop()`
   - Fixed-delay schedule with explicit configurable interval.
5. `ProcessedEventStore` extension (if required)
   - Add optional query by path to support scanner diff decisions:
     `Instant lastProcessedAt(String tenantId, String path)`

### Invariants to preserve

- Tenant isolation:
  - Scanner never traverses outside configured tenant/source roots.
  - Snapshot keys and drift events are tenant-scoped.
- Single processing path:
  - Scanner and watcher both feed the same `EventPipeline`.
- Reconciliation safety:
  - Re-running `runOnce()` without source changes must converge to zero drift.
- Observability:
  - Metrics/logs include metadata only (`tenantId`, counts, durations), no
    document payload.

### Failure-mode handling strategy

- Source tree unavailable during scan:
  - Mark tenant/source as failed for this run; continue scanning others.
- Partial scan failure:
  - Persist per-source checkpoints; do not discard successful source snapshots.
- Large tree/slow scan:
  - Enforce timeout budget per source and emit incomplete-run metrics.
- Event emission failure:
  - Retain drift items in run result for retry on next schedule.

### Migration/rollback notes

- Additive-only PR; no contract-breaking model changes.
- Rollback path: disable `ReconciliationScheduler` startup wiring.
- Scanner state files (snapshots/checkpoints) are local and can be ignored by
  runtime when scanner is disabled.

## Current Target: PR4 design (format profiling + metadata registry)

### Design summary

Add tenant-scoped, directory-level metadata registry with format fingerprinting
and versioned history. Registry must be queryable by tenant+directory and feed
parser-selection hints for later PRs.

### Affected modules/files

- `src/main/java/com/acme/docsync/model/`
  - `DirectoryMetadataProfile.java`
  - `FormatFingerprint.java`
  - `MetadataVersionRecord.java`
- `src/main/java/com/acme/docsync/metadata/`
  - extend `MetadataRegistry.java` interface
  - `FileMetadataRegistry.java` (local persistence)
  - `FormatProfiler.java`
  - `FingerprintCalculator.java`
- `src/main/java/com/acme/docsync/storage/`
  - optional metadata-state path helper (tenant-scoped)
- `src/main/java/com/acme/docsync/app/`
  - `Application.java` wiring for metadata registry/profiler
- `src/test/java/com/acme/docsync/metadata/`
  - registry query/versioning tests
  - fingerprint determinism tests

### Contract/interface changes

1. `MetadataRegistry` expanded API
   - `void upsertProfile(DirectoryMetadataProfile profile)`
   - `DirectoryMetadataProfile getLatest(String tenantId, String directory)`
   - `List<MetadataVersionRecord> history(String tenantId, String directory)`
2. `FormatProfiler`
   - `DirectoryMetadataProfile profile(String tenantId, String directory, ... )`
   - Produces normalized metadata and structural signature.
3. `FingerprintCalculator`
   - `FormatFingerprint compute(ProfileInput input)`
   - Stable hash from sorted structural keys/signature fields.
4. Versioning rule for registry
   - New fingerprint => increment profile version.
   - Same fingerprint => update last-seen timestamp only.

### Invariants to preserve

- Tenant isolation:
  - Metadata records physically and logically tenant-scoped.
- Deterministic fingerprints:
  - Same structural input must produce identical fingerprint.
- Auditability:
  - Version history is append-only for fingerprint changes.
- Safety:
  - Parser hints are allow-list values, not arbitrary executable directives.

### Failure-mode handling strategy

- Invalid metadata payload:
  - Reject write with typed validation error.
- Corrupt metadata store file:
  - Fail current operation with explicit context; do not silently reset history.
- Concurrent updates:
  - Use atomic file write + per-key synchronization to avoid lost updates.
- Unknown/unsupported media type:
  - Persist profile with conservative defaults and explicit warning state.

### Migration/rollback notes

- Additive-only data model and storage.
- Rollback path: disable profiler writes, keep existing read paths unaffected.
- Existing sync pipeline continues to work without metadata profile usage.

## Current Target: PR5 design (parsing, mapping, canonicalization)

### Design summary

Introduce plugin-driven parsing (`detect`, `parse`, `map`, `validate`) and a
canonical transformation pipeline that emits one deterministic output model.
Add provenance mapping (`sourcePath -> canonicalPath`) with version context and
clear validation failures.

### Affected modules/files

- `src/main/java/com/acme/docsync/format/`
  - evolve `FormatPlugin.java` to explicit 4-stage contract
  - `FormatPluginRegistry.java`
  - `FormatDetectionResult.java`
- `src/main/java/com/acme/docsync/transform/`
  - evolve `CanonicalTransformer.java`
  - `CanonicalOutputValidator.java`
  - `CanonicalValidationResult.java`
- `src/main/java/com/acme/docsync/model/`
  - `CanonicalDocument.java`
  - `SourceDocument.java` (or parsed intermediate model)
  - `ProvenanceRecord.java`
- `src/main/java/com/acme/docsync/storage/`
  - `ProvenanceStore.java`
  - `FileProvenanceStore.java`
- `src/main/java/com/acme/docsync/app/`
  - `Application.java` wiring for plugin registry + validator + provenance store
- `src/test/java/com/acme/docsync/format/`
  - plugin detection/parse/map/validate tests
- `src/test/java/com/acme/docsync/transform/`
  - canonical transformer and validator tests
- `src/test/java/com/acme/docsync/storage/`
  - provenance write/read tests

### Contract/interface changes

1. `FormatPlugin` contract (explicit stages)
   - `FormatDetectionResult detect(SourceDocument input, DirectoryMetadataProfile profile)`
   - `ParsedDocument parse(SourceDocument input) throws FormatParseException`
   - `CanonicalDocument map(ParsedDocument parsed) throws TransformException`
   - `void validate(CanonicalDocument canonical) throws TransformException`
2. `FormatPluginRegistry`
   - `void register(String pluginId, FormatPlugin plugin)`
   - `FormatPlugin resolve(SourceDocument input, DirectoryMetadataProfile profile)`
   - Uses metadata hints + detection score with allow-list behavior.
3. `CanonicalTransformer`
   - `CanonicalDocument transform(SourceDocument input, DirectoryMetadataProfile profile)`
   - Internally executes detect/parse/map/validate chain.
4. `CanonicalOutputValidator`
   - `CanonicalValidationResult validate(CanonicalDocument document)`
   - Returns actionable field-level issues, no sensitive payload echoes.
5. `ProvenanceStore`
   - `void put(ProvenanceRecord record)`
   - `ProvenanceRecord findBySource(String tenantId, String sourcePath)`
   - `ProvenanceRecord findByCanonical(String tenantId, String canonicalPath)`

### Invariants to preserve

- Deterministic canonicalization:
  - Equivalent source structures map to identical canonical field layout/order.
- Validation completeness:
  - Canonical output is always validated before persistence/publishing.
- Provenance integrity:
  - Every successful transform writes a source->canonical mapping record.
- Tenant isolation:
  - Plugin resolution, validation, and provenance queries are tenant-scoped.
- Safe plugin dispatch:
  - Registry resolves only registered allow-list plugins.

### Failure-mode handling strategy

- No matching plugin:
  - Return typed detection failure with supported-format context.
- Parse/mapping errors:
  - Bubble typed exception with source path + plugin ID metadata only.
- Canonical validation failure:
  - Return structured violations and skip output persistence.
- Provenance write failure:
  - Fail processing step explicitly; do not silently continue as success.
- Registry misconfiguration:
  - Startup-time validation for duplicate plugin IDs and invalid hints.

### Migration/rollback notes

- Additive interfaces and models; existing sync path remains intact.
- Rollback path: route processing to previous transformer behavior and disable
  new plugin registry wiring.
- Provenance store is local and can be ignored when feature toggle is disabled.

## Current Target: PR6 design (output and index publishing)

### Design summary

Persist canonical documents in tenant-scoped local storage, maintain local index
publish state, and enforce consistent delete semantics (tombstone + optional
physical delete) across both stores.

### Affected modules/files

- `src/main/java/com/acme/docsync/model/`
  - `CanonicalWriteRequest.java`
  - `PublishStateRecord.java`
  - `DeletePolicy.java`
  - `TombstoneRecord.java`
- `src/main/java/com/acme/docsync/storage/`
  - `CanonicalDocumentStore.java`
  - `FileCanonicalDocumentStore.java`
  - `IndexPublishStateStore.java`
  - `FileIndexPublishStateStore.java`
  - `TombstoneStore.java`
  - `FileTombstoneStore.java`
- `src/main/java/com/acme/docsync/transform/` (or `sync/`)
  - `DeleteCoordinator.java`
  - `PublishCoordinator.java`
- `src/main/java/com/acme/docsync/app/`
  - `Application.java` wiring for stores/coordinators
- `src/test/java/com/acme/docsync/storage/`
  - canonical persistence/delete tests
  - index-state parity tests
- `src/test/java/com/acme/docsync/transform/` (or `sync/`)
  - coordinator behavior tests

### Contract/interface changes

1. `CanonicalDocumentStore`
   - `void put(CanonicalDocument doc)`
   - `CanonicalDocument get(String tenantId, String canonicalPath)`
   - `void delete(String tenantId, String canonicalPath, DeletePolicy policy)`
2. `IndexPublishStateStore`
   - `void upsert(PublishStateRecord record)`
   - `PublishStateRecord find(String tenantId, String canonicalPath)`
   - `void markDeleted(String tenantId, String canonicalPath, Instant at)`
3. `TombstoneStore`
   - `void put(TombstoneRecord record)`
   - `TombstoneRecord find(String tenantId, String canonicalPath)`
4. `PublishCoordinator`
   - `void publish(CanonicalDocument doc, String checksum, Instant at)`
   - Writes canonical doc + index state in one logical operation.
5. `DeleteCoordinator`
   - `void applyDelete(String tenantId, String canonicalPath, DeletePolicy policy, Instant at)`
   - Ensures canonical store, tombstone store, and index state stay consistent.

### Invariants to preserve

- Tenant isolation:
  - canonical docs, index state, and tombstones are tenant-scoped.
- Delete consistency:
  - every delete updates index state and tombstone state together.
- Deterministic replay:
  - canonical + provenance + publish state are sufficient to rebuild index feed.
- Safety:
  - physical delete never occurs silently when policy requires tombstone.
- Auditability:
  - tombstone contains delete time and delete reason/policy metadata.

### Failure-mode handling strategy

- Canonical write succeeds but index state fails:
  - return explicit failure and persist recovery marker for retry.
- Delete partially applied:
  - coordinator records compensating action requirement and fails operation.
- Corrupt local state file:
  - fail the request with contextual error; do not auto-reset silently.
- Repeated delete request:
  - treat as safe no-op with idempotent final state.

### Migration/rollback notes

- Additive interfaces and local stores; no external dependency requirement.
- Rollback path: disable new publish/delete coordinators and keep canonical
  transform outputs local-only.
- Existing data remains readable; tombstones can be ignored when rollback mode
  is active.

## Current Target: PR7 design (hardening and release readiness)

### Design summary

Stabilize the full pipeline for production-like operation: improve runtime
resilience, add end-to-end safety checks, strengthen observability, and provide
operational runbooks. No new functional features; quality and reliability only.

### Affected modules/files

- `src/main/java/com/acme/docsync/app/`
  - `Application.java` startup validation and graceful shutdown hooks
- `src/main/java/com/acme/docsync/sync/`
  - hardened watcher/scanner lifecycle handling
  - retry/backoff helpers for transient file IO failures
  - health snapshot/reporting utility
- `src/main/java/com/acme/docsync/storage/`
  - atomic-write helper utility for all file-backed stores
  - consistency checker for canonical/index/tombstone parity
- `src/main/java/com/acme/docsync/transform/`
  - pipeline failure categorization and structured error records
- `src/test/java/com/acme/docsync/`
  - multi-tenant concurrent update scenarios
  - restart/recovery scenarios
  - delete-consistency parity scenarios
- `docs/`
  - operations runbook, failure recovery guide, replay/rebuild guide

### Contract/interface changes

1. `RuntimeHealthReport` (new model)
   - summarizes watcher status, scanner status, queue depth, last reconciliation,
     and failed source count.
2. `ConsistencyChecker` (new service)
   - `ConsistencyReport run()`
   - verifies canonical store, index state, and tombstone state parity.
3. File-store write behavior update
   - all file-backed stores use atomic temp-write + rename semantics.
4. Error classification model
   - categorize failures as `VALIDATION`, `TRANSIENT_IO`, `PERMANENT_IO`,
     `PLUGIN`, `CONSISTENCY`.

### Invariants to preserve

- Tenant isolation remains strict across health/consistency checks.
- No silent failures:
  - every background failure produces structured error metadata.
- Recovery safety:
  - restart must not corrupt local state stores.
- Delete consistency:
  - parity checker must confirm canonical/index/tombstone alignment.
- Backward compatibility:
  - existing persisted files remain readable.

### Failure-mode handling strategy

- Startup misconfiguration:
  - fail fast with actionable configuration error summary.
- Background component crash:
  - auto-restart with bounded backoff and explicit failure counters.
- Atomic write failure:
  - preserve previous state file; never leave partial file in active path.
- Consistency drift detected:
  - emit report and queue corrective replay action; avoid destructive auto-fix.
- Rebuild/replay failure:
  - stop with checkpointed progress and resumable state.

### Migration/rollback notes

- No data model breakage; mostly runtime and test hardening.
- Rollback path: disable hardening wrappers and health checks while preserving
  core processing flow.
- Runbooks should document both hardened mode and fallback mode operations.

