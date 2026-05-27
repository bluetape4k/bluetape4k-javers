# Module bluetape4k-javers-persistence-kafka

[English](./README.md) | 한국어

Kafka로 JaVers CDO snapshot을 발행하는 모듈입니다. 이 모듈은 의도적으로
write-only입니다. Snapshot을 Kafka record로 직렬화해 downstream system이 audit
event를 소비할 수 있게 하며, repository read method는 빈 값이나 기본값을 반환합니다.

## 아키텍처

![Kafka persistence sequence](docs/images/readme-diagrams/javers-kafka-sequence-01.png)

## 기능

- Encoded JaVers snapshot을 발행하는 `KafkaCdoSnapshotRepository`.
- Kafka topic과 key mapping 설정.
- write-only contract가 보이도록 첫 read path에서 warning log 출력, 반복 메시지는 debug로 완화.
- Kafka send 실패 propagation.
- `javers-core` codec 재사용.

## 사용 예

```kotlin
val repository = KafkaCdoSnapshotRepository(
    producer = producer,
    topic = "order-audit-events",
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Kafka를 audit event stream으로 사용할 때 이 모듈을 사용하세요. 애플리케이션이
history read도 필요하다면 durable snapshot repository나 projection consumer와
함께 사용해야 합니다.

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
