# Document Sync Platform

Java starter project for a multi-tenant document sync and normalization
platform focused on search and analytics.

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

The process wires up storage and sync components and prints
`Document Sync Platform initialized.` State under `./data` (for example
`./data/state/`) is used when you extend ingestion and watchers.

## Modules (package-level)

- `storage`: tenant-isolated file storage contract
- `metadata`: directory-level metadata and format profile
- `sync`: hybrid sync interfaces (events + reconciliation)
- `format`: extensible format parsing and fingerprinting
- `transform`: canonical output transformation
- `model`: domain objects
- `app`: bootstrap entry point
