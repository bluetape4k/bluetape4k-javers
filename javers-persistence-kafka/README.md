# Module bluetape4k-javers-persistence-kafka

English | [한국어](./README.ko.md)

Kafka-backed JaVers CDO snapshot publisher. This module is intentionally
write-only: it serializes snapshots to Kafka records so downstream systems can
consume audit events, while repository read methods return empty/default values.

## Architecture

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## Features

- `KafkaCdoSnapshotRepository` for Spring Kafka `KafkaTemplate` publishing.
- `VanillaKafkaCdoSnapshotRepository` for Spring Kafka API-free Apache Kafka `Producer` publishing.
- Transport-neutral `CdoSnapshotEvent` metadata contract shared by Kafka publisher adapters.
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
`Producer<String, String>` or wants to avoid wiring through Spring Kafka APIs:

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

You can also let the repository create a repository-owned producer from Kafka
configuration through the governed `bluetape4k-kafka` `producerOf(...)` helper:

```kotlin
val repository = VanillaKafkaCdoSnapshotRepository(
    mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        ProducerConfig.ACKS_CONFIG to "all",
    ),
    options,
)
```

When the repository creates the producer, it also owns and closes it. When you
pass a `Producer<String, String>` directly, producer lifecycle remains
caller-owned unless `closeProducerOnClose = true` is set.

Use this module when Kafka is the audit event stream. Pair it with a durable
snapshot repository or a projection consumer when the application also needs
historical reads.

## Snapshot Event Pipeline

Both Kafka repositories now build a `CdoSnapshotEvent<String>` before publishing.
The event carries:

- global id value
- commit id, major id, and minor id
- nullable repository sequence
- snapshot version and type
- author and commit timestamp
- codec id and opaque idempotency key

`repositorySequence` is nullable because JaVers assigns repository sequence
after `saveSnapshot()` succeeds. Transport adapters should treat the
idempotency key as opaque and must propagate publish failures so JaVers does not
advance the audit-log head after a failed event publish.

## Adapter Selection

| Adapter | Runtime dependency | Use when |
|---|---|---|
| `KafkaCdoSnapshotRepository` | Spring Kafka `KafkaTemplate` supplied by the caller. | The application already uses Spring Kafka. |
| `VanillaKafkaCdoSnapshotRepository` | Apache Kafka `Producer<String, String>` supplied by the caller, or `producerOf(...)` from governed `bluetape4k-kafka` config. | The application is not Spring-based or owns producer lifecycle directly. |

`VanillaKafkaCdoSnapshotRepository.close()` does not close the producer unless
`closeProducerOnClose = true` is set. Keep the default when an application-level
Kafka client lifecycle already owns the producer.

## Planned Non-Kafka Adapters

The core event contract is transport-neutral, but this module implements Kafka
only.

| Planned adapter | Current status | Notes |
|---|---|---|
| NATS JetStream | Design artifact only. | Publish acknowledgement, subject mapping, and metadata headers must be defined before implementation. |
| Amazon SQS | Design artifact only. | FIFO group/deduplication settings must stay queue-type-specific, and an AWS SDK dependency decision is still separate. |

Kafka write-only publishing remains separate from read-side projection work
(#105) and durable-history plus event-stream composition (#131).

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
