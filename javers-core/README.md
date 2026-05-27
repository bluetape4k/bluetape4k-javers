# Module bluetape4k-javers-core

English | [한국어](./README.ko.md)

[JaVers](https://javers.org) extension layer for Kotlin services. This module
contains the shared codec, repository, JQL, metamodel, and convenience APIs used
by every other `bluetape4k-javers` module.

## Features

- Kotlin extension functions for JaVers comparison, snapshot, shadow, and JQL
  workflows.
- `JaversCodec` implementations for string, binary, and compressed snapshot
  payloads.
- `AbstractCdoSnapshotRepository`, the shared base for cache, Redis, Kafka, and
  Exposed snapshot repositories.
- Cache-backed repository implementations for Caffeine, Cache2k, and JCache.
- Snowflake commit id generation support for cluster-friendly commit metadata.

## Usage

```kotlin
val javers = JaversBuilder.javers()
    .build()

val diff = javers.compare(oldOrder, newOrder)
val snapshot = javers.latestSnapshotOrNull<Order>(orderId)
```

Use `javers-core` directly when the application needs JaVers helper APIs without
a SQL, Redis, or Kafka persistence adapter.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-core")
}
```

## Build

```bash
./gradlew :javers-core:test
```
