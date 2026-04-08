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

## Canonical output format

Current canonical model (`CanonicalDocument`) contains:

- `tenantId` (`String`)
- `canonicalPath` (`String`)
- `fields` (`Map<String, Object>`)

For person-profile style data, the canonical payload in `fields` should follow
this normalized structure:

```json
{
  "entity_type": "person_profile",
  "person": {
    "name": {
      "first": "John",
      "last": "Michel",
      "full": "John Michel"
    },
    "education": [
      {
        "degree_type": "masters",
        "institution": {
          "name": "University Name",
          "country": "USA",
          "state": "CA"
        },
        "primary_subject": "Computer science",
        "secondary_subjects": ["Maths", "Business Accounts"]
      }
    ],
    "experience": [
      {
        "start_date": "2002-11-10",
        "end_date": "2006-06-12",
        "title": "Software Engineer",
        "organization": {
          "name": "IBM",
          "country": "USA",
          "state": "CA",
          "city": "SFO"
        }
      }
    ],
    "skills": [
      {
        "category": "programming_language",
        "values": ["java", "python"]
      }
    ]
  }
}
```

### Example mapping: input shape A -> canonical

```json
{
  "entity_type": "person_profile",
  "person": {
    "name": {
      "first": "John",
      "last": "Michel",
      "full": "John Michel"
    },
    "education": [
      {
        "degree_type": "masters",
        "institution": {
          "name": "San Jose State University",
          "country": "USA",
          "state": "CA"
        },
        "primary_subject": "Computer science",
        "secondary_subjects": ["Maths", "Business Accounts"]
      },
      {
        "degree_type": "bachelors",
        "institution": {
          "name": "San Jose State University",
          "country": "USA",
          "state": "CA"
        },
        "primary_subject": "Maths",
        "secondary_subjects": ["Computer Science", "Physics"]
      }
    ],
    "experience": [
      {
        "start_date": "2002-11-10",
        "end_date": "2006-06-12",
        "title": "Software Engineer",
        "organization": {
          "name": "IBM",
          "country": "USA",
          "state": "CA",
          "city": "SFO"
        }
      },
      {
        "start_date": "2006-06-20",
        "end_date": "2021-11-12",
        "title": "Senior Software Engineer",
        "organization": {
          "name": "Microsoft",
          "country": "USA",
          "state": "CA",
          "city": "SFO"
        }
      }
    ],
    "skills": [
      {"category": "uncategorized", "values": ["java", "python"]},
      {"category": "databases", "values": ["Databases"]},
      {"category": "others", "values": ["Distributed Systems"]}
    ]
  }
}
```

### Example mapping: input shape B -> canonical

```json
{
  "entity_type": "person_profile",
  "person": {
    "name": {
      "first": "John",
      "last": "Michel",
      "full": "John Michel"
    },
    "education": [
      {
        "degree_type": "masters",
        "institution": {
          "name": "California University of Pennsylvania",
          "country": "USA",
          "state": "CA"
        },
        "primary_subject": "Computer science",
        "secondary_subjects": ["Maths", "Business Accounts"]
      },
      {
        "degree_type": "bachelors",
        "institution": {
          "name": "Indian Institute of Technology",
          "country": "INDIA",
          "state": "Tamil Nadu"
        },
        "primary_subject": "Computer Science",
        "secondary_subjects": ["Maths", "English"]
      }
    ],
    "experience": [
      {
        "start_date": "2001-07-15",
        "end_date": "2005-09-12",
        "title": "SE II",
        "organization": {
          "name": "google",
          "country": "USA",
          "state": "CA",
          "city": "SFO"
        }
      },
      {
        "start_date": "2005-10-01",
        "end_date": "2021-03-11",
        "title": "Principal Engineer",
        "organization": {
          "name": "uber",
          "country": "USA",
          "state": "CA",
          "city": "SFO"
        }
      }
    ],
    "skills": [
      {"category": "programming_language", "values": ["Go", "python"]},
      {"category": "databases", "values": ["MySQL", "Postgres", "Oracle"]},
      {
        "category": "others",
        "values": [
          "Software Engineering",
          "Distributed Systems",
          "Cloud services",
          "Datastructures",
          "Algorithms"
        ]
      }
    ]
  }
}
```
