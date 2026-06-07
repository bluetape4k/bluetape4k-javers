# Module bluetape4k-javers-persistence-redis

English | [한국어](./README.ko.md)

Redis-backed JaVers CDO snapshot repositories for applications that want audit
history in Redis instead of the built-in JaVers stores.

## Architecture

![Redis persistence sequence](docs/images/readme-diagrams/javers-redis-sequence-01.png)

## Features

- `LettuceCdoSnapshotRepository` for Lettuce-based Redis access.
- `RedissonCdoSnapshotRepository` for Redisson-based Redis access.
- Encoded snapshot storage keyed by JaVers global id.
- Commit id sequence tracking so repository heads survive instance rebuilds.
- Shared codec support from `javers-core`.

## Usage

```kotlin
val repository = LettuceCdoSnapshotRepository(
    name = "orders",
    client = redisClient,
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Use this module when Redis is the audit snapshot store. For query-side CQRS read
models, keep application projections separate from the JaVers snapshot data.

## Repository selection

Both repositories share the same JaVers snapshot contract: snapshots are queried
newest-first, commit id sequence metadata is persisted so the repository head can
be rebuilt, and codec failures propagate without advancing the head commit.

Choose `LettuceCdoSnapshotRepository` when the application already standardizes
on Lettuce, needs explicit Redis command/transaction control, or wants the
smallest Redis client abstraction around the JaVers audit store.

Choose `RedissonCdoSnapshotRepository` when the application already standardizes
on Redisson, benefits from higher-level Redis collections, or plans to evaluate
Redisson near-cache and read/write-through strategies in a follow-up layer.

This module intentionally does not define a provider-neutral cache contract.
Near-cache, read-through, write-through, and write-behind behavior should reuse
the existing bluetape4k cache and Exposed cache modules before JaVers-specific
mapping code is added.

When Exposed is the durable audit store, use `javers-exposed` for canonical
snapshot history and apply Redisson/Lettuce cache strategies to read models or
projections through `bluetape4k-exposed`. Do not use this Redis repository as a
write-behind cache for SQL-backed audit writes, commit sequence metadata, or
repository head restoration.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-redis")
}
```

## Build

```bash
./gradlew :javers-persistence-redis:test
```

## References

- [JaVers](https://javers.org)
- [Redis](https://redis.io/)
