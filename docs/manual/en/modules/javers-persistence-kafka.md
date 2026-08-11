# javers-persistence-kafka

`javers-persistence-kafka` turns JaVers snapshot writes into Kafka records through Spring Kafka's `KafkaTemplate`. It is a synchronous, write-only publisher shaped as a `JaversRepository`; it is not a readable audit repository.

## Dependency and when to choose it

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-kafka")
    implementation("org.springframework.kafka:spring-kafka")
}
```

Spring Kafka is an optional surface in this module, so the application must add it. Choose this adapter when another system consumes encoded `CdoSnapshot` records and the command path intentionally waits for broker acknowledgement. Do not choose it as the only repository when the application needs snapshots, changes, shadows, or restart head recovery.

## Runnable quick start

```kotlin
import io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

data class Order(@Id val id: Long, val status: String)

val producerFactory = DefaultKafkaProducerFactory<String, String>(
    mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"),
    StringSerializer(),
    StringSerializer(),
)
val kafkaTemplate = KafkaTemplate<String, String>(producerFactory, true).also {
    it.setDefaultTopic("javers.order-snapshots")
}
try {
    val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
    val javers = JaversBuilder.javers()
        .registerJaversRepository(repository)
        .registerEntity(Order::class.java)
        .build()

    javers.commit("order-service", Order(1, "PLACED"))
} finally {
    producerFactory.destroy()
}
```

`saveSnapshot` uses `sendDefault(GlobalId, encodedSnapshot)` and waits for the returned future for up to 30 seconds by default. Configure the template's default topic before the first commit. The record key is the snapshot GlobalId and the value is uncompressed JSON produced by `JaversCodecs.String`. The exact contract is in [`KafkaCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt).

Keep the producer factory alive while the repository can publish. The `finally` block is appropriate for a short standalone process; a managed application should destroy the factory during application shutdown, after in-flight commits have finished.

## Publisher and repository responsibility

All read-side methods return empty, false, or zero and log a warning. A repository instance remembers its in-memory head only after a successful local commit; a rebuilt instance has no head because Kafka is never read. Since previous state is unavailable, repeated commits can be treated as initial snapshots rather than meaningful diffs.

JaVers calls `saveSnapshot` once per snapshot. A commit that produces several snapshots results in several blocking sends followed by a local head update. Kafka acknowledgement proves that the producer completed that send according to its configuration. It does not prove that a consumer processed the record or that the whole JaVers commit was published atomically.

## Failure modes and delivery semantics

Timeout, interruption, and producer errors become `RuntimeException`; interruption restores the thread interrupt flag. Earlier records from the same JaVers commit may already be accepted when a later send fails. Retrying the command may publish duplicates. Release 0.3.0 supplies no producer transaction, outbox, consumer, replay coordinator, deduplication key beyond GlobalId, or exactly-once workflow.

Set producer `acks`, idempotence, retries, delivery timeout, topic partitions, retention, and ACLs explicitly. If ordering per aggregate matters, keep GlobalId as the key and verify partition behavior. Monitor send latency, timeouts, error rate, topic lag, dead-letter handling, and consumer projection drift. Treat the payload schema and codec as a versioned integration contract.

## Read-side projection contract

`KafkaCdoSnapshotProjector` replays the encoded snapshot value into an existing
read-capable `CdoSnapshotRepository`. The current wire value does not contain the
source repository sequence, so the projector requires the snapshot topic to have
exactly one partition. It checks the topic topology before the first poll and
throws `IllegalStateException` before decoding, projecting, or committing offsets
when the topic is multi-partition. A successful topology check is reused for the
projector lifetime.

This guard prevents target-local poll order from silently changing the global
JaVers head. Multi-partition replay requires a separately versioned wire-visible
monotonic sequence contract; changing the topic partition count alone is not
enough. Keep the existing `partition, offset` batch order and commit offsets only
after the complete batch has been projected successfully.

The module integration test uses `bluetape4k-testcontainers` through the shared
Kafka test provider and verifies real-broker replay into a Redis projection. Keep
that Testcontainers check alongside mock regression tests when changing the
projector.

## Testing

```bash
./gradlew :javers-persistence-kafka:test
```

[`KafkaCdoSnapshotRepositoryTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt) verifies successful publication, failed-future propagation, and absent head restoration. Application tests should consume the record and assert topic, key, payload decoding, duplicate handling, and recovery after a partial multi-snapshot commit.

## Non-goals

- It is not a readable or durable JaVers query repository.
- It is not a Kafka consumer or CQRS projection implementation.
- It does not provide exactly-once delivery or atomic multi-snapshot publication.
- It does not create or configure topics.

Related reading: [Kafka persistence](../persistence/kafka.md), [failure contracts](../operations/failure-contracts.md), and the [DDD/CQRS guide](../guides/ddd-and-cqrs.md).
