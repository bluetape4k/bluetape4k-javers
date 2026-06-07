# Module bluetape4k-javers-persistence-kafka

English | [한국어](./README.ko.md)

Kafka-backed JaVers CDO snapshot publisher. This module is intentionally
write-only: it serializes snapshots to Kafka records so downstream systems can
consume audit events, while repository read methods return empty/default values.

## Architecture

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## Features

- `KafkaCdoSnapshotRepository` for Spring Kafka `KafkaTemplate` publishing.
- `VanillaKafkaCdoSnapshotRepository` for Spring-free Apache Kafka `Producer` publishing.
- Configurable Kafka topic, key mapping, publish timeout, flush behavior, and producer lifecycle ownership.
- Explicit first read-path warning, with repeated write-only contract messages demoted to debug.
- Failure propagation from Kafka send operations.
- Codec reuse from `javers-core`.

## Usage

### Spring Kafka

```kotlin
val repository = KafkaCdoSnapshotRepository(
    kafkaOperations = kafkaTemplate,
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

### Vanilla Kafka

Use the vanilla repository when the application already owns an Apache Kafka
`Producer<String, String>` and should not depend on Spring Kafka at runtime:

```kotlin
val options = VanillaKafkaCdoSnapshotRepositoryOptions(
    topic = "order-audit-events",
    publishTimeout = Duration.ofSeconds(10),
)

val repository = VanillaKafkaCdoSnapshotRepository(
    producer = producer,
    options = options,
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

You can create the producer with raw Kafka clients or an optional
`bluetape4k-kafka` / `bluetape4k-kafka4` helper that matches the application's
Kafka compatibility line:

```kotlin
val producer = producerOf<String, String>(
    mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        ProducerConfig.ACKS_CONFIG to "all",
    ),
    StringSerializer(),
    StringSerializer(),
)
```

That helper dependency is optional; `javers-persistence-kafka` does not require
it on the production runtime classpath.

Use this module when Kafka is the audit event stream. Pair it with a durable
snapshot repository or a projection consumer when the application also needs
historical reads.

## Adapter Selection

| Adapter | Runtime dependency | Use when |
|---|---|---|
| `KafkaCdoSnapshotRepository` | Spring Kafka `KafkaTemplate` supplied by the caller. | The application already uses Spring Kafka. |
| `VanillaKafkaCdoSnapshotRepository` | Apache Kafka `Producer<String, String>` supplied by the caller. | The application is not Spring-based or owns producer lifecycle directly. |

`VanillaKafkaCdoSnapshotRepository.close()` does not close the producer unless
`closeProducerOnClose = true` is set. Keep the default when an application-level
Kafka client lifecycle already owns the producer.

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
