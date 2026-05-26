# Module bluetape4k-javers-exposed

English | [한국어](./README.ko.md)

Exposed JDBC persistence for [JaVers](https://javers.org) CDO snapshots.
This module provides `ExposedCdoSnapshotRepository`, an `AbstractCdoSnapshotRepository`
implementation that stores JaVers commits and encoded snapshots in SQL tables
managed through JetBrains Exposed.

## Features

- Stores one row per JaVers commit in `javers_commit`.
- Stores one row per CDO snapshot version in `javers_snapshot`.
- Persists the full encoded `CdoSnapshot` payload so JaVers can reconstruct
  snapshots with its configured JSON converter.
- Restores the repository head commit id after a repository instance rebuild.
- Supports H2, PostgreSQL, and MySQL through Exposed JDBC.

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

## Query Behavior

The first implementation delegates JaVers query filtering to the inherited
repository behavior after loading snapshots by global id. SQL pushdown is out of
scope for this module version and can be added behind the same repository API
later.

## Build

```bash
./gradlew :javers-exposed:test
```

## References

- [JaVers](https://javers.org)
- [JetBrains Exposed](https://www.jetbrains.com/exposed/)
