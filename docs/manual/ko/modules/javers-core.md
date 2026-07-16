# javers-core

`javers-core`는 JaVers를 Kotlin에서 쓰기 편하게 다듬은 공통 모듈입니다. 쿼리 확장, 코덱, 변경 내역 도우미와 Caffeine·Cache2k·JCache 기반 `CdoSnapshotRepository`를 제공합니다. 객체 차이만 계산하거나 프로세스 안에서 감사 테스트를 돌릴 때는 이 모듈만으로 충분합니다. 재시작 뒤에도 이력을 남겨야 한다면 영속 저장 모듈을 추가하세요.

## 의존성과 핵심 API

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-core")
}
```

`CdoSnapshotRepository`는 JaVers `JaversRepository`를 확장해 스냅샷 한 건 저장과 GlobalId별 최신순 조회를 정의합니다. `AbstractCdoSnapshotRepository`는 모든 어댑터가 공유하는 쿼리 필터, 코덱, head 커밋 관리, 저장 순서를 구현합니다. [`CdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepository.kt)와 [`AbstractCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt)가 실제 계약입니다.

## 바로 실행하는 예제

```kotlin
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id

data class Order(@Id val id: Long, var status: String)

val repository = CaffeineCdoSnapshotRepository()
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

val order = Order(1, "PLACED")
javers.commit("order-service", order)
order.status = "PAID"
javers.commit("order-service", order)

val changes = javers.findChanges(queryByInstanceId<Order>(1L))
check(changes.isNotEmpty())
```

쿼리 확장은 [`QueryBuilderExtensions.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/jql/QueryBuilderExtensions.kt)에 있고, 같은 커밋·조회 흐름을 [`CommitAndQueryExamples.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/test/kotlin/io/bluetape4k/javers/examples/CommitAndQueryExamples.kt)에서 검증합니다.

## 저장 방식과 실패 경계

캐시 저장소는 인코딩한 스냅샷과 커밋 순서를 현재 프로세스에 둡니다. 캐시가 밀려나거나 프로세스가 재시작하면 이력을 잃고, JaVers가 기존 객체를 새 객체로 판단할 수도 있습니다. 테스트, 예제, 크기가 제한된 캐시처럼 이력을 버려도 되는 경우에만 맞습니다.

`AbstractCdoSnapshotRepository.persist`는 저장소 인스턴스 하나를 잠근 뒤 스냅샷을 한 건씩 저장하고 마지막에 head와 sequence를 갱신합니다. 중간에 실패하면 뒤 작업은 멈추지만 이미 외부 저장소에 쓴 내용까지 되돌리지는 않습니다. 범위가 넓은 JQL 조회는 `getAll()`에서 모든 스냅샷을 메모리에 올립니다. 키가 10,000개를 넘을 때 경고할 뿐이므로 운영 쿼리는 범위를 좁히고 heap 사용량을 확인해야 합니다.

코덱은 손상된 페이로드를 `null`로 돌려줄 수 있고, 저장소는 `mapNotNull`로 이를 제외합니다. 조회 전체가 실패하는 대신 일부 이력이 빠질 수 있다는 뜻입니다. 코덱 오류를 관측하고 운영과 같은 코덱으로 복원 테스트를 돌리세요.

## 운영과 테스트

저장소 수명, 캐시 최대 크기, eviction, 코덱 버전, 커밋 ID 순서를 운영 설정으로 관리합니다. 캐시 저장소는 rolling restart만 해도 빈 상태에서 시작합니다.

```bash
./gradlew :javers-core:test
```

릴리스 테스트는 커밋, 스냅샷, shadow, 코덱과 캐시 구현 세 가지를 검증합니다. 애플리케이션 테스트에서는 같은 객체를 두 번 커밋한 뒤 `findChanges`, 최신순 스냅샷, shadow 복원을 확인하세요.

## 하지 않는 일

- 도메인 객체를 저장하지 않습니다.
- 캐시 저장소는 영속 감사 이력을 보장하지 않습니다.
- 0.2.1에는 여러 스냅샷 저장소를 합치는 기능이 없습니다.
- 쿼리 확장을 써도 메모리 필터가 데이터 저장소의 쿼리로 바뀌지는 않습니다.

다음은 [감사 모델](../architecture/audit-model.md), [테스트 안내](../guides/testing.md), [영속 방식 선택](../persistence/selection-guide.md)입니다.
