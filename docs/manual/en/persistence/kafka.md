# Kafka persistence

`KafkaCdoSnapshotRepository` is a write-only JaVers repository adapter. It publishes each encoded `CdoSnapshot` to Kafka; it is not a relational snapshot store and cannot answer JaVers history queries.

```kotlin
// Configure "javers-snapshots" as kafkaTemplate's default topic first.
val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

`saveSnapshot` encodes the snapshot as JSON, calls `sendDefault` with `snapshot.globalId.value()` as the record key, and waits up to 30 seconds for the result. The `KafkaTemplate` must therefore have a default topic. Send failure, timeout, and interruption propagate as runtime failures; interruption restores the thread interrupt flag first. `getKeys`, `contains`, `getSeq`, `getSnapshotSize`, and `loadSnapshots` log a warning and return empty/default values. `getHeadId` cannot be restored after rebuilding the adapter. These boundaries are explicit in [`KafkaCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt) and its [`failure test`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt).

Kafka ordering is scoped to a partition. A stable GlobalId key normally routes one object's snapshots to one partition, but producer retries, consumer processing, topic configuration, and downstream storage still determine end-to-end behavior. Release 0.2.1 makes no exactly-once claim and provides no consumer or projection repair protocol for this repository.

Use this adapter when Kafka publication itself is the required JaVers repository side effect and consumers understand the encoded snapshot schema. If the application also needs JaVers queries, choose Exposed or Redis as the repository and publish events separately under an explicit failure strategy. See [repository composition](../architecture/repository-composition.md).
