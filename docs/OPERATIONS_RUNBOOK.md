# Operations Runbook

This runbook describes how to operate and recover the local document sync
pipeline.

## Key runtime components

- Ingress: `NioFileWatcherAdapter`, `WatcherEventMapper`, `EventPipeline`,
  `EventIngressQueue`
- Reconciliation: `ReconciliationScheduler`, `ReconciliationScanner`,
  `DriftDetector`
- Metadata/transform: `FormatProfiler`, `MetadataRegistry`,
  `PluginCanonicalTransformer`
- Publish/delete state: `PublishCoordinator`, `DeleteCoordinator`,
  canonical/index/tombstone stores
- Operational checks: `RuntimeHealthService`, `ConsistencyChecker`

## Runtime health checks

- Verify startup initializes:
  - ingestion validation and queue/pipeline
  - reconciliation scanner/scheduler
  - metadata/transform/publish-delete dependencies
- Use `RuntimeHealthService` to inspect:
  - watcher running state
  - reconciliation scheduler running state
  - ingress queue depth
  - failed source count
- Investigate immediately when:
  - queue depth grows continuously
  - scheduler is stopped unexpectedly
  - failed source count increases repeatedly

## Consistency checks

- Use `ConsistencyChecker` per tenant/canonical path to verify parity across:
  - canonical document store
  - index publish state store
  - tombstone store
- A consistent deleted record requires:
  - index state marked deleted
  - tombstone present
- A physically deleted canonical document should not still exist when
  tombstone delete policy is physical.

## Recovery procedures

- If ingress backs up or stalls:
  - check watcher health and queue depth
  - drain via `EventPipeline.drainAndMarkProcessed()`
  - verify `ProcessedEventStore` is writable
- If a store write fails:
  - inspect error output for path and operation context
  - retry operation (file-backed writes use atomic temp-file + replace)
- If reconciliation source fails:
  - verify source root exists and is readable
  - rerun scanner after source permissions/path fixes
- If consistency drift is reported:
  - replay publish/delete operation for affected canonical path
  - rerun `ConsistencyChecker` to confirm closure
- If transform fails:
  - validate plugin registration in `FormatPluginRegistry`
  - verify source payload is parseable for detected plugin
  - rerun transform and validate canonical output errors

## Restart guidance

- Stop services gracefully.
- Keep local `data/state` files; they are required for continuity:
  - processed events
  - snapshots
  - metadata profiles/history
  - publish/index/tombstone/provenance state
- Restart application and confirm health snapshot returns expected values.
- Run a post-restart spot check:
  - execute one ingest event path end-to-end
  - run one reconciliation cycle
  - run one `ConsistencyChecker` parity check
