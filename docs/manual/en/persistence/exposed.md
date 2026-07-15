# Exposed persistence

Choose `ExposedCdoSnapshotRepository` when JaVers history belongs in a relational database already operated by the service. It stores audit data; it does not replace application repositories from `bluetape4k-exposed`.

```kotlin
val auditRepository = ExposedCdoSnapshotRepository(database)
auditRepository.ensureSchema()
val javers = JaversBuilder.javers()
    .registerJaversRepository(auditRepository)
    .build()
```

`CdoSnapshotTable` maps to `javers_snapshot` and stores GlobalId, commit ID, version, snapshot type, encoded state, changed properties, and managed type. It has a unique `(global_id, version)` index. `CommitTable` maps to `javers_commit` and stores author, timestamps, properties, and the repository-local sequence used to restore the head commit. The exact schema is in [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt).

## Transaction and consistency boundary

Every method calls `transaction(database)` or the current default `transaction`. During a JaVers commit, `saveSnapshot` starts a transaction for each snapshot; the inherited `persist` later updates the commit sequence in another transaction. Application state written by a subclass such as the example `OrderRepository` also uses its own transaction. Release 0.2.1 therefore does not make domain state, all audit snapshots, and commit sequence one database transaction.

A failure propagates to the caller, but earlier committed operations may remain. The unique index blocks duplicate GlobalId/version rows; it is not a general retry protocol. Production code should decide whether to use an outbox, explicit orchestration, or reconciliation.

`ensureSchema()` calls `SchemaUtils.create`. Use it in tests and local startup when appropriate. In production, version and deploy the two tables with the service's migration process, and grant the runtime only the required DML permissions. The [bluetape4k-exposed transaction guide](https://bluetape4k.github.io/manual/bluetape4k-exposed/1.11/modules/bluetape4k-exposed-jdbc/transaction-ownership/) covers application repository ownership; this adapter remains responsible only for JaVers CDO snapshots.

The repository implementation is pinned at [`ExposedCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt). Continue with [testing](../guides/testing.md).
