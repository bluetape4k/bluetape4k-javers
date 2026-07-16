# javers-persistence-redis

`javers-persistence-redis` provides separate Lettuce and Redisson implementations of `CdoSnapshotRepository`. Choose it when Redis itself is the audit snapshot store and the service accepts Redis durability, eviction, and memory policy as part of the audit contract.

## Dependency and client choice

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-redis")
    implementation("io.lettuce:lettuce-core") // choose this
    // implementation("org.redisson:redisson") // or this
}
```

The Redis clients are optional compile-time surfaces in the module build, so the application must add the client it uses. Lettuce uses a dedicated synchronous connection and a lock around `MULTI/EXEC` for each snapshot write. Redisson uses `RListMultimap` for snapshots and `RMap` for commit sequences. Do not configure both merely because both repository classes exist.

## Lettuce quick start

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379")
val repository = LettuceCdoSnapshotRepository(
    name = "orders",
    client = redisClient,
)
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

Lettuce stores newest-first snapshot bytes in `javers:{name}:snapshot:{globalId}`, a GlobalId index in `javers:{name}:globalId:set`, and commit sequences in `javers:{name}:sequence:set`. The snapshot list push and GlobalId index update share one Redis transaction. The later sequence update from the inherited persist loop is separate. See [`LettuceCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt).

## Redisson quick start

```kotlin
val config = Config().apply {
    useSingleServer().address = "redis://127.0.0.1:6379"
}
val redisson = Redisson.create(config)
val repository = RedissonCdoSnapshotRepository(
    name = "orders",
    redisson = redisson,
)
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

Redisson stores per-GlobalId snapshot lists in `javers:{name}:snapshot` and commit sequences in `javers:{name}:sequence`. A snapshot append and the later sequence update are separate remote operations. The implementation is [`RedissonCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt).

Both repositories default to the LZ4/Fory codec, return newest snapshots first, and scan the sequence map to restore the latest head after reconstruction.

## Failure modes and operations

Lettuce propagates transaction failures and attempts `DISCARD`, but a network failure can make the client uncertain whether Redis applied a command. Redisson does not wrap snapshot append and sequence update in one transaction. In both cases partial audit state is possible, and retrying a whole commit can append a duplicate snapshot.

Decoding uses `mapNotNull`; corrupt or incompatible bytes may disappear from query results. Redis eviction can remove snapshot lists, indexes, or sequence entries independently. Configure persistence, eviction, replication, backup, keyspace alerts, memory headroom, and a codec-upgrade test according to the required audit retention. Give each environment and bounded context an intentional repository `name` to avoid key collisions.

## Testing

```bash
./gradlew :javers-persistence-redis:test
```

The release runs Lettuce and Redisson commit/shadow suites against Redis Testcontainers and verifies head restoration after repository reconstruction in [`LettuceJaversCommitTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceJaversCommitTest.kt) and [`RedissonJaversCommitTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonJaversCommitTest.kt).

## Non-goals

- It is not a CQRS projection API.
- It is not an append-only event log.
- It does not supply cross-key or whole-commit exactly-once semantics.
- It does not choose Redis persistence, eviction, cluster, or backup policy.

Related reading: [Redis persistence](../persistence/redis.md), [failure contracts](../operations/failure-contracts.md), and [observability](../operations/observability.md).
