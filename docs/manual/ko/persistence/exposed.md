# Exposed 영속 저장

서비스가 운영하는 관계형 DB에 JaVers 이력을 남겨야 한다면 `ExposedCdoSnapshotRepository`를 선택합니다. 이 모듈은 감사 데이터를 저장하며 `bluetape4k-exposed`의 애플리케이션 repository를 대신하지 않습니다.

```kotlin
val auditRepository = ExposedCdoSnapshotRepository(database)
auditRepository.ensureSchema()
val javers = JaversBuilder.javers()
    .registerJaversRepository(auditRepository)
    .build()
```

`CdoSnapshotTable`은 `javers_snapshot`에 GlobalId, 커밋 ID, 버전, 유형, 인코딩한 상태, 변경 프로퍼티, 관리 유형을 저장합니다. `(global_id, version)`에는 고유 인덱스가 있습니다. `CommitTable`은 `javers_commit`에 작성자, 시각, 프로퍼티와 저장소 내부 순서 값을 저장합니다. 이 값으로 저장소를 다시 만들 때 head 커밋을 복원합니다. 실제 스키마는 [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt)에 있습니다.

## Transaction 경계

메서드마다 `transaction(database)` 또는 기본 `transaction`을 엽니다. JaVers 커밋 안에서도 스냅샷 하나마다 트랜잭션이 생기고, 모든 스냅샷을 저장한 뒤 커밋 순서를 별도 트랜잭션에서 갱신합니다. 예제의 `OrderRepository`가 업무 데이터를 저장하는 트랜잭션도 따로입니다. 업무 상태, 감사 스냅샷 전체, 커밋 순서는 하나의 DB 트랜잭션으로 묶이지 않습니다.

오류는 호출자에게 전달되지만 먼저 끝난 작업까지 되돌리지는 않습니다. 재시도할 때 unique index만으로 전체 멱등성을 보장할 수 없습니다. 운영에서는 outbox, 명시적 orchestration, reconciliation 가운데 필요한 방식을 정해야 합니다.

`ensureSchema()`는 테스트와 로컬 실행에 쓰고, 운영 테이블은 migration 절차로 관리하세요. 애플리케이션 저장소와 트랜잭션 소유권은 [bluetape4k-exposed 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/1.11/modules/bluetape4k-exposed-jdbc/transaction-ownership/)에 있습니다. 구현은 [`ExposedCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt)를 참고하세요.
