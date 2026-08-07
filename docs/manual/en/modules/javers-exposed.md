# javers-exposed

`javers-exposed` stores JaVers commit metadata and encoded CDO snapshots in relational tables through Exposed JDBC. Choose it when audit history must survive service restarts and the team already operates a relational database.

## Dependency and schema

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-exposed")
    runtimeOnly("org.postgresql:postgresql")
}
```

`ExposedCdoSnapshotRepository` is the module's primary API. `CdoSnapshotTable` maps to `javers_snapshot` with a unique `(global_id, version)` index. `CommitTable` maps to `javers_commit` and stores author, timestamps, properties, and the repository-local sequence used to restore the head. See [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt).

## Runnable quick start: initialize and register

```kotlin
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.jetbrains.exposed.v1.jdbc.Database

data class Order(@Id val id: Long, val status: String)

val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/app",
    driver = "org.postgresql.Driver",
    user = "app",
    password = "secret",
)

val order = Order(1, "PLACED")
val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

`ensureSchema()` invokes `SchemaUtils.create(CommitTable, CdoSnapshotTable)`. It is useful for tests and local startup. Production should version the same schema with the service's migration process and run the application with DML-only permissions where possible. The implementation is pinned in [`ExposedCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt).

## Persistence and query semantics

Every repository operation runs in `transaction(database)` or the current default Exposed transaction. `saveSnapshot` inserts commit metadata if absent and then inserts one encoded snapshot. The inherited persist loop saves snapshots one by one and updates the commit sequence afterward in another transaction. Domain data, every audit snapshot, and the head sequence are therefore not one database transaction in 0.3.0.

Snapshots for one GlobalId are returned by descending version. Broader JaVers queries inherit the core repository's `getAll()` filtering and do not push filters into SQL. Plan indexes and retention for the current schema, but do not assume that an arbitrary JQL query becomes a bounded SQL query.

## Failure modes and operations

The unique index rejects the same GlobalId/version twice, but does not make a whole JaVers commit idempotent. A failure can leave domain state, commit metadata, or earlier snapshots without the final sequence update. Reconcile by GlobalId, version, and commit ID; use an outbox or explicit orchestration if another resource must move with the audit write.

Monitor transaction failures, duplicate-key errors, row growth, large payloads, query latency, and a head that cannot be restored. Back up both tables together. A codec change must preserve the ability to decode existing `state` values.

## Testing

```bash
./gradlew :javers-exposed:test
```

[`ExposedCdoSnapshotRepositoryH2Test.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryH2Test.kt) verifies schema use, newest-first history, query filtering, snapshot lookup, and head restoration. Run database smoke tests against the same vendor and migration used in production.

## Non-goals

- It does not persist application domain objects.
- It does not make application and audit transactions atomic.
- It does not provide SQL pushdown for general JaVers queries.
- `ensureSchema()` is not a migration ledger.

Related reading: [Exposed persistence](../persistence/exposed.md), [failure contracts](../operations/failure-contracts.md), and [testing](../guides/testing.md).
