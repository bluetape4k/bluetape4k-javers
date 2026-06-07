# Module bluetape4k-javers-exposed

English | [한국어](./README.ko.md)

Exposed JDBC persistence for [JaVers](https://javers.org) CDO snapshots.
This module provides `ExposedCdoSnapshotRepository`, an `AbstractCdoSnapshotRepository`
implementation that stores JaVers commits and encoded snapshots in SQL tables
managed through JetBrains Exposed.

## Features

- Stores one row per JaVers commit in `javers_commit`.
- Stores one row per CDO snapshot version in `javers_snapshot`.
- Allows repository-local table names when the default names collide with an
  existing schema or tenant layout.
- Persists the full encoded `CdoSnapshot` payload so JaVers can reconstruct
  snapshots with its configured JSON converter.
- Keeps a unique `(global_id, version)` index for the hot snapshot history load
  path and an index on commit sequence for repository head restoration.
- Pushes common JaVers snapshot filters into SQL before decoding snapshot JSON.
- Restores the repository head commit id after a repository instance rebuild.
- Supports H2, PostgreSQL, and MySQL through Exposed JDBC.

## Architecture

![javers-exposed architecture](docs/images/readme-diagrams/javers-exposed-architecture-01.png)

`ExposedCdoSnapshotRepository` extends the shared
`AbstractCdoSnapshotRepository` codec and query behavior, then maps JaVers
commit metadata and encoded `CdoSnapshot` rows to Exposed tables.

## Persistence Flow

![javers-exposed persistence sequence](docs/images/readme-diagrams/javers-exposed-sequence-01.png)

Writes run inside an Exposed transaction: the repository inserts commit metadata
when needed, stores the encoded snapshot payload, and later decodes rows ordered
by snapshot version for JaVers queries.

## Usage

```kotlin
val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/app",
    driver = "org.postgresql.Driver",
    user = "app",
    password = "secret",
)

val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Use repository options when migrations own the schema or when the default table
names are not acceptable:

```kotlin
val options = ExposedCdoSnapshotRepositoryOptions(
    tableNames = ExposedJaversTableNames(
        commitTableName = "audit_commit",
        snapshotTableName = "audit_snapshot",
    ),
    createSchemaOnEnsure = false,
)

val repository = ExposedCdoSnapshotRepository(
    database = database,
    options = options,
)
```

With `createSchemaOnEnsure = false`, `ensureSchema()` becomes a no-op and the
application must create the mapped tables through Exposed `SchemaUtils`, Flyway,
Liquibase, or another migration tool before writing snapshots.

## Query Behavior

`ExposedCdoSnapshotRepository` avoids a full repository scan for common JaVers
snapshot queries:

- `getStateHistory(globalId, queryParams)` loads only the requested global id
  when the query does not include aggregate child value objects.
- `getStateHistory(classes, queryParams)` prefilters by persisted managed type
  when the query does not include aggregate child value objects.
- `getSnapshots(queryParams)` pushes exact commit ids, exact version, exact
  author, `LocalDateTime` commit date range, snapshot type, `skip`, and `limit`
  into the SQL query before decoding snapshot JSON.

Queries that need JaVers in-memory semantics still fall back to the shared
repository filtering path. This includes aggregate child value object queries,
changed-property filters, commit-property filters, author-like matching,
`Instant` commit date ranges, version ranges, `toCommitId`, and
`snapshotQueryLimit`.

## Build

```bash
./gradlew :javers-exposed:test
```

## References

- [JaVers](https://javers.org)
- [JetBrains Exposed](https://www.jetbrains.com/exposed/)
