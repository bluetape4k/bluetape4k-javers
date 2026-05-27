# Module bluetape4k-javers-persistence-kafka

English | [한국어](./README.ko.md)

Kafka-backed JaVers CDO snapshot publisher. This module is intentionally
write-only: it serializes snapshots to Kafka records so downstream systems can
consume audit events, while repository read methods return empty/default values.

## Architecture

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## Features

- `KafkaCdoSnapshotRepository` for publishing encoded JaVers snapshots.
- Configurable Kafka topic and key mapping.
- Explicit first read-path warning, with repeated write-only contract messages demoted to debug.
- Failure propagation from Kafka send operations.
- Codec reuse from `javers-core`.

## Usage

```kotlin
val repository = KafkaCdoSnapshotRepository(
    producer = producer,
    topic = "order-audit-events",
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Use this module when Kafka is the audit event stream. Pair it with a durable
snapshot repository or a projection consumer when the application also needs
historical reads.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-kafka")
}
```

## Build

```bash
./gradlew :javers-persistence-kafka:test
```

## References

- [JaVers](https://javers.org)
- [Apache Kafka](https://kafka.apache.org/)
