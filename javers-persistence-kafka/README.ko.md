# Module bluetape4k-javers-persistence-kafka

[English](./README.md) | 한국어

Kafka로 JaVers CDO snapshot을 발행하는 모듈입니다. 이 모듈은 의도적으로
write-only입니다. Snapshot을 Kafka record로 직렬화해 downstream system이 audit
event를 소비할 수 있게 하며, repository read method는 빈 값이나 기본값을 반환합니다.

## 아키텍처

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## 기능

- Spring Kafka `KafkaTemplate`로 encoded JaVers snapshot을 발행하는 `KafkaCdoSnapshotRepository`.
- Spring Kafka API 없이 Apache Kafka `Producer`로 snapshot을 발행하는 `VanillaKafkaCdoSnapshotRepository`.
- Kafka publisher adapter가 공유하는 transport-neutral `CdoSnapshotEvent` metadata contract.
- Kafka topic, key mapping, publish timeout, flush 동작, producer lifecycle ownership 설정.
- write-only contract가 보이도록 첫 read path에서 warning log 출력, 반복 메시지는 debug로 완화.
- Kafka send 실패 propagation.
- `javers-core` codec 재사용.

## 사용 예

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

애플리케이션이 Apache Kafka `Producer<String, String>`를 직접 소유하거나 Spring
Kafka API로 wiring하지 않으려면 vanilla repository를 사용합니다:

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

Repository가 governed `bluetape4k-kafka` `producerOf(...)` helper로 producer를
생성하게 할 수도 있습니다:

```kotlin
val repository = VanillaKafkaCdoSnapshotRepository(
    mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        ProducerConfig.ACKS_CONFIG to "all",
    ),
    options,
)
```

Repository가 producer를 생성하면 producer를 소유하고 닫습니다. `Producer<String,
String>`를 직접 넘기면 `closeProducerOnClose = true`를 설정하지 않는 한 producer
lifecycle은 호출자가 소유합니다.

Kafka를 audit event stream으로 사용할 때 이 모듈을 사용하세요. 애플리케이션이
history read도 필요하다면 durable snapshot repository나 projection consumer와
함께 사용해야 합니다.

## Snapshot Event Pipeline

두 Kafka repository는 publish 전에 `CdoSnapshotEvent<String>`을 생성합니다. Event는
다음 metadata를 포함합니다:

- global id value
- commit id, major id, minor id
- nullable repository sequence
- snapshot version과 type
- author와 commit timestamp
- codec id와 opaque idempotency key

`repositorySequence`는 JaVers가 `saveSnapshot()` 성공 뒤에 repository sequence를
배정하기 때문에 nullable입니다. Transport adapter는 idempotency key를 opaque 값으로
취급해야 하며, publish 실패를 propagation해서 실패한 event publish 뒤 JaVers audit-log
head가 advance되지 않도록 해야 합니다.

## Adapter 선택 기준

| Adapter | Runtime dependency | 사용 시점 |
|---|---|---|
| `KafkaCdoSnapshotRepository` | 호출자가 제공하는 Spring Kafka `KafkaTemplate`. | 애플리케이션이 이미 Spring Kafka를 사용할 때. |
| `VanillaKafkaCdoSnapshotRepository` | 호출자가 제공하는 Apache Kafka `Producer<String, String>` 또는 governed `bluetape4k-kafka` config 기반 `producerOf(...)`. | Spring 기반이 아니거나 producer lifecycle을 직접 소유할 때. |

`VanillaKafkaCdoSnapshotRepository.close()`는 `closeProducerOnClose = true`를
설정하지 않으면 producer를 닫지 않습니다. 애플리케이션 수준 Kafka client lifecycle이
producer를 이미 소유한다면 기본값을 유지하세요.

## 예정된 Non-Kafka Adapter

Core event contract는 transport-neutral이지만, 이 모듈은 Kafka만 구현합니다.

| 예정 adapter | 현재 상태 | 참고 |
|---|---|---|
| NATS JetStream | Design artifact only. | 구현 전 publish acknowledgement, subject mapping, metadata header 계약이 필요합니다. |
| Amazon SQS | Design artifact only. | FIFO group/deduplication 설정은 queue type별로 분리해야 하며, AWS SDK dependency 결정은 별도입니다. |

Kafka write-only publishing은 read-side projection 작업(#105), durable history와
event stream composition 작업(#131)과 분리되어 있습니다.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-kafka")
}
```

## 빌드

```bash
./gradlew :javers-persistence-kafka:test
```

## 참고 자료

- [JaVers](https://javers.org)
- [Apache Kafka](https://kafka.apache.org/)
