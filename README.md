# Document Sync Platform

Java project for a multi-tenant document sync and normalization platform
focused on canonicalization, reconciliation, and operational safety.

## Prerequisites

- **JDK 21** (matches `maven.compiler.source` / `target` in `pom.xml`)
- **Apache Maven 3.8+** (3.9.x recommended)

Verify:

```bash
java -version   # should report 21
mvn -version
```

## Build

From the repository root:

```bash
mvn compile
```

## Run tests

```bash
mvn test
```

## Run the application

Compile, then start the main class (no extra runtime JARs are required):

```bash
mvn compile
java -cp target/classes com.acme.docsync.app.Application
```

The process wires up ingestion, reconciliation, metadata, transform,
publish/delete, and health components, then prints
`Document Sync Platform initialized.` Local state under `./data/state/` is
used for restart continuity.

## Modules (package-level)

- `app`: bootstrap and dependency wiring (`Application`)
- `model`: domain records/enums/exceptions (ingestion, canonical, metadata,
  publish/delete state)
- `storage`: tenant path policy/resolver plus file-backed stores
  (canonical/index/tombstone/provenance/processed/snapshot)
- `sync`: watcher ingress (`NioFileWatcherAdapter`), queue/pipeline
  (`EventIngressQueue`, `EventPipeline`), reconciliation, runtime health
- `metadata`: directory profiling/fingerprints and metadata version registry
- `format`: plugin contracts and detection/parse abstractions
- `transform`: plugin-backed canonical transform plus publish/delete
  coordinators

## Runtime interactions

1. Watchers produce `RawWatcherEvent`; mapper converts to `IngestionContract`.
2. `EventPipeline` validates and enqueues, then drains and marks processed.
3. Reconciliation runs on schedule and re-emits drift corrections through the
   same pipeline.
4. Metadata profiling updates directory profiles and version history.
5. Transform path resolves a format plugin and produces validated canonical
   output.
6. Publish/delete coordinators update canonical, index, and tombstone state.
7. Health/parity checks are exposed via `RuntimeHealthService` and
   `ConsistencyChecker`.

## Documentation

- Class definitions/interactions: `docs/CLASS_OVERVIEW.md`
- Operations and recovery: `docs/OPERATIONS_RUNBOOK.md`
