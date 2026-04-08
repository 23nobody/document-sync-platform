# Operations Runbook

This runbook describes how to operate and recover the local document sync
pipeline.

## Runtime health checks

- Verify startup initializes:
  - ingestion validation
  - watcher/scanner components
  - metadata/transform/publish stores
- Use `RuntimeHealthService` to inspect:
  - watcher running state
  - reconciliation scheduler running state
  - ingress queue depth
  - failed source count

## Consistency checks

- Use `ConsistencyChecker` per tenant/canonical path to verify parity across:
  - canonical document store
  - index publish state store
  - tombstone store
- A consistent deleted record requires:
  - index state marked deleted
  - tombstone present

## Recovery procedures

- If a store write fails:
  - inspect error output for path and operation context
  - retry operation (writes are atomic temp-file + replace)
- If reconciliation source fails:
  - verify source root exists and is readable
  - rerun scanner after source permissions/path fixes
- If consistency drift is reported:
  - replay publish/delete operation for affected canonical path
  - rerun `ConsistencyChecker` to confirm closure

## Restart guidance

- Stop services gracefully.
- Keep local `data/state` files; they are required for continuity:
  - processed events
  - snapshots
  - metadata profiles/history
  - publish/index/tombstone/provenance state
- Restart application and confirm health snapshot returns expected values.
