# javers-persistence-kafka

`javers-persistence-kafka`는 JaVers 스냅샷 쓰기를 Spring Kafka `KafkaTemplate`의 레코드 발행으로 바꿉니다. `JaversRepository` 형태를 빌린 동기식 write-only publisher이며, 감사 이력을 읽는 저장소가 아닙니다.

## 의존성과 선택 기준

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-kafka")
    implementation("org.springframework.kafka:spring-kafka")
}
```

Spring Kafka는 이 모듈의 선택 의존성이므로 애플리케이션에 직접 추가해야 합니다. 다른 시스템이 인코딩한 `CdoSnapshot` 레코드를 소비하고, command 경로가 broker 응답을 기다리도록 설계했다면 선택할 수 있습니다. 애플리케이션이 스냅샷, change, shadow를 조회하거나 재시작 뒤 head를 복원해야 한다면 이 어댑터 하나만 등록해서는 안 됩니다.

## 바로 실행하는 예제

```kotlin
val producerFactory = DefaultKafkaProducerFactory(
    mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"),
    StringSerializer(),
    StringSerializer(),
)
val kafkaTemplate = KafkaTemplate(producerFactory, true).also {
    it.setDefaultTopic("javers.order-snapshots")
}
val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

`saveSnapshot`은 `sendDefault(GlobalId, encodedSnapshot)`을 호출하고 반환된 future를 기본 30초까지 기다립니다. 첫 커밋 전에 template의 기본 토픽을 지정해야 합니다. 레코드 키는 스냅샷 GlobalId이고 값은 `JaversCodecs.String`이 만든 압축하지 않은 JSON입니다. 정확한 계약은 [`KafkaCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt)에 있습니다.

## 발행자와 저장소의 책임

읽기 메서드는 모두 빈 값, `false`, `0`을 반환하고 경고 로그를 남깁니다. 저장소 인스턴스는 현재 프로세스에서 커밋이 성공한 뒤에만 head를 기억합니다. 새 인스턴스는 Kafka를 읽지 않으므로 head가 없습니다. 이전 상태도 읽지 못하기 때문에 같은 객체를 다시 커밋해도 의미 있는 diff 대신 초기 스냅샷으로 처리될 수 있습니다.

JaVers는 스냅샷마다 `saveSnapshot`을 한 번 호출합니다. 스냅샷이 여러 개인 커밋은 여러 번 동기 발행한 뒤 로컬 head를 갱신합니다. Kafka 응답은 producer 설정에 따라 해당 send가 끝났다는 뜻입니다. consumer가 처리했다거나 JaVers 커밋 전체가 원자적으로 발행됐다는 뜻은 아닙니다.

## 실패와 전달 방식

timeout, interruption, producer 오류는 `RuntimeException`으로 전달하며 interruption이면 thread interrupt flag를 되살립니다. 같은 JaVers 커밋의 앞 레코드는 성공하고 뒤 발행만 실패할 수 있습니다. command를 다시 실행하면 중복 레코드가 생길 수 있습니다. 0.2.1에는 producer transaction, outbox, consumer, replay 조정, 중복 제거 저장소, exactly-once 흐름이 없습니다.

producer의 `acks`, idempotence, retry, delivery timeout과 토픽 partition, retention, ACL을 명시하세요. aggregate별 순서가 중요하면 GlobalId 키를 유지하고 partition 동작을 검증합니다. 발행 지연, timeout, 오류율, consumer lag, dead letter 처리, projection drift를 관측하세요. 페이로드 스키마와 코덱은 버전이 있는 연동 계약으로 다뤄야 합니다.

## 테스트

```bash
./gradlew :javers-persistence-kafka:test
```

[`KafkaCdoSnapshotRepositoryTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt)는 발행 성공, 실패한 future의 예외 전달, head를 복원하지 못하는 계약을 검증합니다. 애플리케이션 테스트에서는 실제 레코드를 소비해 토픽, 키, 페이로드 디코딩, 중복 처리, 여러 스냅샷 중 일부만 발행된 뒤의 복구를 확인하세요.

## 하지 않는 일

- 조회 가능한 JaVers 저장소가 아닙니다.
- Kafka consumer나 CQRS projection을 구현하지 않습니다.
- exactly-once 전달이나 여러 스냅샷의 원자적 발행을 보장하지 않습니다.
- 토픽을 만들거나 설정하지 않습니다.

이어서 [Kafka 영속 저장](../persistence/kafka.md), [실패 계약](../operations/failure-contracts.md), [DDD/CQRS 안내](../guides/ddd-and-cqrs.md)를 읽어 보세요.
