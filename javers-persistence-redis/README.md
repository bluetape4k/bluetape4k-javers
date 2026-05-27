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
    commands = redisCommands,
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Use this module when Redis is the audit snapshot store. For query-side CQRS read
models, keep application projections separate from the JaVers snapshot data.

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
