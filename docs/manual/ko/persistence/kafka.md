# Kafka 영속 경로

`KafkaCdoSnapshotRepository`는 쓰기 전용 JaVers 어댑터입니다. 인코딩한 `CdoSnapshot`을 Kafka로 발행하지만 관계형 스냅샷 저장소가 아니며 JaVers 이력 조회에도 쓸 수 없습니다.

```kotlin
// kafkaTemplate에 "javers-snapshots"를 default topic으로 먼저 설정합니다.
val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

`saveSnapshot`은 plain JSON으로 인코딩한 뒤 GlobalId를 레코드 키로 삼아 `sendDefault`를 호출합니다. 결과는 최대 30초 기다립니다. send 실패, timeout, interrupt는 실행 오류로 전달되며 interrupt flag는 복원합니다. `getKeys`, `contains`, `getSeq`, `getSnapshotSize`, `loadSnapshots`는 경고를 남기고 빈 값이나 0을 돌려줍니다. 어댑터를 새로 만들면 head도 복원하지 못합니다. 정확한 계약은 [`KafkaCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt)와 [`KafkaCdoSnapshotRepositoryTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt)에 있습니다.

Kafka 순서는 partition 범위에서 해석해야 합니다. 같은 GlobalId 키는 보통 같은 partition으로 가지만 producer 재시도, 소비자 처리, topic 설정, 프로젝션 저장까지 합친 exactly-once 계약은 없습니다. 여러 스냅샷을 발행하다 실패하면 앞부분만 남고, 재시도하면 중복될 수 있습니다.

Kafka 발행 자체가 저장소의 목적이고 소비자가 스냅샷 스키마를 이해할 때만 이 어댑터를 고르세요. JaVers 조회도 필요하면 Exposed나 Redis를 저장소로 두고 별도 실패 전략 아래 이벤트를 발행해야 합니다. [저장소 조합](../architecture/repository-composition.md)을 함께 읽으세요.
