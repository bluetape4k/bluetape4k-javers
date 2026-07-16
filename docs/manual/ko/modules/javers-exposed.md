# javers-exposed

`javers-exposed`는 JaVers 커밋 메타데이터와 인코딩한 CDO 스냅샷을 Exposed JDBC로 관계형 DB에 저장합니다. 서비스가 재시작해도 감사 이력을 남겨야 하고 팀이 이미 관계형 DB를 운영한다면 이 모듈이 맞습니다.

## 의존성과 스키마

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-exposed")
    runtimeOnly("org.postgresql:postgresql")
}
```

주요 API는 `ExposedCdoSnapshotRepository`입니다. `CdoSnapshotTable`은 `javers_snapshot`에 대응하며 `(global_id, version)` 고유 인덱스가 있습니다. `CommitTable`은 `javers_commit`에 작성자, 시각, 속성, 최신 커밋 복원에 쓰는 저장소 내부 순서 값을 기록합니다. 실제 테이블 정의는 [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt)에서 확인할 수 있습니다.

## 바로 실행하는 예제: 스키마 생성과 등록

```kotlin
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.jetbrains.exposed.v1.jdbc.Database

data class Order(@Id val id: Long, val status: String)

val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/app",
    driver = "org.postgresql.Driver",
    user = "app",
    password = "secret",
)

val order = Order(1, "PLACED")
val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

`ensureSchema()`는 `SchemaUtils.create(CommitTable, CdoSnapshotTable)`를 호출합니다. 테스트와 로컬 시작에는 편리하지만, 운영에서는 같은 스키마를 서비스의 마이그레이션 절차로 버전 관리하세요. 가능하면 애플리케이션 계정에는 DML 권한만 줍니다. 구현은 [`ExposedCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt)에 고정돼 있습니다.

## 저장과 조회 방식

저장소 메서드는 `transaction(database)`나 현재 기본 Exposed 트랜잭션에서 동작합니다. `saveSnapshot`은 커밋 메타데이터가 없으면 먼저 넣고, 인코딩한 스냅샷 한 건을 저장합니다. 상위 클래스의 저장 흐름은 스냅샷을 한 건씩 쓴 뒤 별도 트랜잭션에서 커밋 순서를 갱신합니다. 0.2.1에서는 도메인 데이터, 모든 감사 스냅샷, 최신 커밋 순서가 한 DB 트랜잭션으로 묶이지 않습니다.

GlobalId 하나를 조회하면 버전 역순으로 돌려줍니다. 범위가 넓은 JaVers 쿼리는 core 저장소의 `getAll()` 필터를 그대로 쓰며 SQL로 조건을 밀어 넣지 않습니다. 현재 스키마의 인덱스와 보존 정책은 설계할 수 있지만, 임의의 JQL이 제한된 SQL로 바뀐다고 가정해서는 안 됩니다.

## 실패와 운영

고유 인덱스는 같은 GlobalId/버전 중복을 막지만 JaVers 커밋 전체를 멱등하게 만들지는 않습니다. 실패 시 도메인 상태, 커밋 메타데이터, 앞서 저장한 스냅샷만 남고 마지막 순서 값이 빠질 수 있습니다. GlobalId, 버전, 커밋 ID로 대조하세요. 다른 자원과 함께 움직여야 한다면 아웃박스나 명시적인 조정 절차가 필요합니다.

트랜잭션 실패, 중복 키, 행 증가량, 큰 페이로드, 조회 지연, 최신 커밋 복원 실패를 관측합니다. 두 테이블은 함께 백업하세요. 코덱을 바꿀 때도 기존 `state` 값을 계속 읽을 수 있어야 합니다.

## 테스트

```bash
./gradlew :javers-exposed:test
```

[`ExposedCdoSnapshotRepositoryH2Test.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryH2Test.kt)는 스키마, 최신순 이력, 쿼리 필터, 스냅샷 조회, 최신 커밋 복원을 확인합니다. 운영에서 쓰는 DB와 마이그레이션으로 스모크 테스트도 돌리세요.

## 하지 않는 일

- 애플리케이션 도메인 객체를 저장하지 않습니다.
- 애플리케이션 트랜잭션과 감사 트랜잭션을 원자적으로 묶지 않습니다.
- 일반 JaVers 쿼리를 SQL로 내려 보내지 않습니다.
- `ensureSchema()`는 마이그레이션 이력 관리 도구가 아닙니다.

이어서 [Exposed 영속 저장](../persistence/exposed.md), [실패 계약](../operations/failure-contracts.md), [테스트 안내](../guides/testing.md)를 읽어 보세요.
