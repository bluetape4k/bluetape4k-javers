# Module bluetape4k-javers-persistence-kafka

[English](./README.md) | 한국어

Kafka로 JaVers CDO snapshot을 발행하는 모듈입니다. 이 모듈은 의도적으로
write-only입니다. Snapshot을 Kafka record로 직렬화해 downstream system이 audit
event를 소비할 수 있게 하며, repository read method는 빈 값이나 기본값을 반환합니다.

## 아키텍처

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## 기능

- Spring Kafka `KafkaTemplate`로 encoded JaVers snapshot을 발행하는 `KafkaCdoSnapshotRepository`.
- Spring 없이 Apache Kafka `Producer`로 snapshot을 발행하는 `VanillaKafkaCdoSnapshotRepository`.
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

애플리케이션이 Apache Kafka `Producer<String, String>`를 직접 소유하고 Spring
Kafka runtime dependency를 원하지 않을 때 vanilla repository를 사용합니다:

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

Producer는 raw Kafka client나 애플리케이션 Kafka compatibility line에 맞는 선택적
`bluetape4k-kafka` / `bluetape4k-kafka4` helper로 만들 수 있습니다:

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

이 helper dependency는 선택 사항입니다. `javers-persistence-kafka` production
runtime classpath에는 필요하지 않습니다.

Kafka를 audit event stream으로 사용할 때 이 모듈을 사용하세요. 애플리케이션이
history read도 필요하다면 durable snapshot repository나 projection consumer와
함께 사용해야 합니다.

## Adapter 선택 기준

| Adapter | Runtime dependency | 사용 시점 |
|---|---|---|
| `KafkaCdoSnapshotRepository` | 호출자가 제공하는 Spring Kafka `KafkaTemplate`. | 애플리케이션이 이미 Spring Kafka를 사용할 때. |
| `VanillaKafkaCdoSnapshotRepository` | 호출자가 제공하는 Apache Kafka `Producer<String, String>`. | Spring 기반이 아니거나 producer lifecycle을 직접 소유할 때. |

`VanillaKafkaCdoSnapshotRepository.close()`는 `closeProducerOnClose = true`를
설정하지 않으면 producer를 닫지 않습니다. 애플리케이션 수준 Kafka client lifecycle이
producer를 이미 소유한다면 기본값을 유지하세요.

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
