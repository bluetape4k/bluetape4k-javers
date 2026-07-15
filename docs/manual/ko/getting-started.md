# 시작하기

먼저 Javers, Exposed, Redis, Kafka 버전을 각각 고르는 일부터 하지 마세요. Javers `0.2.1`을 포함한 `bluetape4k-dependencies` 생태계 버전 하나를 정하고, 서비스에 필요한 모듈만 선언합니다. 이 platform이 저장소별 BOM과 공통 라이브러리 버전을 함께 맞춥니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<ecosystem-version>"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

조직 사정으로 생태계 platform을 쓸 수 없다면 `bluetape4k-javers-bom:0.2.1`을 좁은 대안으로 사용할 수 있습니다. 이 경우 저장소 사이의 호환성은 애플리케이션이 확인해야 합니다.

## 가장 작은 영속 저장소

```kotlin
val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

`ensureSchema()`는 `javers_commit`, `javers_snapshot` 테이블을 만듭니다. 로컬 실행과 테스트에는 편리하지만 운영 스키마 변경까지 맡기는 migration 도구는 아닙니다. 저장소 메서드마다 Exposed transaction을 열기 때문에 애플리케이션의 도메인 transaction과 JaVers 저장이 자동으로 하나의 원자 작업이 되지도 않습니다.

정확한 구현은 [`ExposedCdoSnapshotRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt)와 [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt)에서 확인할 수 있습니다. 다음은 [감사 모델](architecture/audit-model.md)과 [영속 방식 선택](persistence/selection-guide.md)입니다.
