# Module bluetape4k-javers-ddd

[English](./README.md) | 한국어

`javers-ddd`는 aggregate root를 이미 저장하고 있는 서비스가 JaVers audit
history와 트랜잭션 경계를 인식하는 domain-event 발행을 함께 붙일 수 있도록 돕는
작은 DDD helper layer입니다. 이 모듈은 source-of-truth repository를 대체하지 않습니다. Repository
subclass가 여전히 persistence와 lookup을 책임지고, 이 모듈은 그 workflow 주변에
JaVers commit과 event publisher 단계를 추가합니다.


## 외부 aggregate 선택적 감사 adapter

`AggregateAuditAdapter`는 추출 함수로 Exposed aggregate를 그대로 감사합니다. 두 marker를
구현하거나 Exposed 의존성을 javers-ddd production에 추가할 필요가 없습니다. 다음 코드는
Exposed를 이미 사용하는 소비자에서 적용합니다. `order`, `persist`, `eventPublisher`는 애플리케이션 소유입니다.

```kotlin
import io.bluetape4k.exposed.core.ddd.AggregateRoot as ExposedAggregate
import io.bluetape4k.exposed.core.ddd.DomainEvent as ExposedEvent
import io.bluetape4k.javers.ddd.AggregateAuditAdapter
import io.bluetape4k.javers.ddd.AuditCompletion
import io.bluetape4k.javers.ddd.DomainEvent

val adapter = AggregateAuditAdapter<ExposedAggregate<Long>, ExposedEvent<Long>>(
    eventsOf = { it.domainEvents() },
    clearEvents = { it.clearDomainEvents() },
    eventMapper = { original ->
        object : DomainEvent {
            override val aggregateId = original.aggregateId
            override val occurredOn = original.occurredAt
            override val eventType = original.javaClass.name
            override val attributes = emptyMap<String, String>()
        }
    },
)
val registration = adapter.capture(order)
try {
    transaction(database) {
        persist(order)
        registration.audit(javers, author)
    }
} catch (failure: Exception) {
    registration.complete(AuditCompletion.UNKNOWN)
    throw failure
}
registration.publish { eventPublisher(it) }
registration.complete(AuditCompletion.COMMITTED)
```

Mapper는 원래 ID/발생 시각/type과 필요한 attributes를 보존해야 합니다. 예제는 추가 attributes가 없는
이벤트를 가정합니다. 다중 이벤트 속성은 기존 collection encoder를 재사용합니다.

호출 순서는 capture → source transaction 안의 audit → transaction 성공 반환 → publish →
complete(COMMITTED)입니다. rollback/결과 불명은 ROLLED_BACK/UNKNOWN으로 종료하며 buffer를 유지합니다.
JaVers backend의 source transaction 참여 여부는 설정에 달려 있고 분산 원자성을 보장하지 않습니다.
발행 실패 후 새 등록 재시도는 중복 전달할 수 있으므로 소비자가 멱등성을 보장해야 합니다.

이벤트는 깊은 불변 값이며 aggregate와 buffer는 완료까지 한 호출자가 고정해야 합니다. callback은
상태를 변경하지 않고 clear는 원자적으로 전체 제거하거나 변경 없이 실패해야 합니다. 참조 순서 검사는
객체 내부 변경을 탐지하지 않으며 부분 clear 복구는 지원하지 않습니다. 캡처 참조는 `events`에 남습니다.
모든 callback은 동기 실행하고 O(N) 캡처/변환 비용이 듭니다. 호출자가 실행 스레드, I/O timeout, 최대 이벤트
수와 재시도 횟수를 정합니다. 자동 재시도, transaction, outbox, dispatcher를 생성하지 않습니다.

## Class Diagram

![javers-ddd class diagram](../docs/images/readme-diagrams/javers-ddd-class-diagram-01.png)

## Save Flow

![javers-ddd save flow](../docs/images/readme-diagrams/javers-ddd-save-flow-01.png)

## 핵심 책임

- `AggregateRoot<ID>`는 audit 대상 aggregate root를 표시하고 안정적인 `id`를
  노출합니다.
- `DomainEvent`는 aggregate가 발생시킨 event를 표현하고, event metadata를 JaVers
  commit property로 매핑합니다.
- `AggregateRepository<T, ID>`는 subclass persistence로 aggregate를 저장한 뒤,
  저장된 상태를 JaVers에 commit하고 두 단계가 성공하면 event를 발행합니다.
- `DomainEventPublisher`는 synchronous fail-fast publisher contract입니다.
- 기본 publisher로 no-op, Kotlin function, composite fan-out, Spring application
  event, Spring Kafka, NATS adapter를 제공합니다.

## 사용 예

```kotlin
data class Order(
    @Id
    override val id: Long,
    var status: String,
) : AggregateRoot<Long>

data class OrderPlaced(
    override val aggregateId: Long,
    override val occurredOn: Instant = Instant.now(),
) : DomainEvent

class OrderRepository(javers: Javers) :
    AggregateRepository<Order, Long>(Order::class.java, javers) {

    override fun persist(aggregate: Order): Order {
        // Exposed, Spring Data, 또는 다른 source-of-truth store에 저장합니다.
        return aggregate
    }

    override fun findById(id: Long): Order? = null
}

val repository = OrderRepository(javers)
repository.save(Order(1, "PLACED"), "system", OrderPlaced(1))
val history = repository.loadHistory(1, limit = 20)
```

`loadHistory`는 snapshot을 최신순으로 반환하고 양수인 `limit`을 JaVers query에
전달합니다. 생략하면 기본 limit 100을 사용하며, API 요청에서 더 작은 history
window를 노출할 때 bounded overload를 사용하세요.

Aggregate id에는 JaVers `@Id`를 붙이고, `Javers`를 만들 때 aggregate type을
entity로 등록하세요:

```kotlin
val javers = JaversBuilder.javers()
    .registerJaversRepository(exposedSnapshotRepository)
    .registerEntity(Order::class.java)
    .build()
```

## Publisher 선택

서비스 경계에 맞는 가장 작은 publisher를 선택하세요:

| Publisher | 사용할 때 |
|---|---|
| `NoopDomainEventPublisher` | JaVers commit metadata만 필요할 때 |
| `FunctionDomainEventPublisher` | 테스트나 작은 애플리케이션에서 lambda로 발행할 때 |
| `CompositeDomainEventPublisher` | 여러 local publisher를 순서대로 실행할 때 |
| `SpringApplicationEventDomainEventPublisher` | Spring listener가 in-process event를 소비할 때 |
| `KafkaDomainEventPublisher` | Spring Kafka로 domain event를 topic에 보낼 때 |
| `NatsDomainEventPublisher` | NATS connection으로 직렬화된 event payload를 발행할 때 |

Spring과 Kafka publisher는 Spring transaction synchronization이 활성화되어 있으면
`afterCommit`에서 발행합니다. 그렇지 않으면 즉시 발행합니다. Kafka는
`publishTimeout`까지 broker acknowledgement를 기다리며, 전송 실패·timeout·interrupt를
즉시 호출 또는 트랜잭션 완료에서 전달하고 rollback에서는 전송하지 않습니다. NATS는
consumer가 제공한 subject resolver와 serializer를 사용해 repository 호출 안에서
동기적으로 발행합니다.

## 전달 의미

`AggregateRepository`는 source persistence와 JaVers commit이 성공한 뒤 event를
발행합니다. Local publisher는 즉시 실행하지만 Spring과 Kafka publisher는
`afterCommit`까지 미룰 수 있으며, Kafka acknowledgement 실패는 문서화한 완료
시점에서 관찰됩니다. 이 기능은 durable outbox 구현이 아닙니다. 정확히 한 번 외부
전달, replay, cross-service recovery가 필요하다면 transactional outbox를 사용하세요.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

Spring, Kafka, NATS adapter는 optional surface입니다. 해당 adapter를 사용할 때만
맞는 runtime dependency를 추가하세요.

## 빌드

```bash
./gradlew :javers-ddd:test
```

### 다중 이벤트 commit properties

빈 이벤트 컬렉션은 properties를 만들지 않습니다. 단일 이벤트는 기존
`domainEventType`, `aggregateId`, `occurredOn`, `event.<attribute>` 키를 유지합니다.
다중 이벤트는 `domainEventCount`, `domainEventTypes`를 유지하고 각 이벤트의 properties를
`events.<0부터 시작하는 index>.` 아래에 저장합니다. index는 입력 컬렉션의 순회 순서입니다.
`events.0.event.tenant`는 `events.0.aggregateId`나 다른 이벤트의 tenant를 덮어쓰지 않습니다.
자동 절단이나 크기 제한은 적용하지 않으므로 호출자는 backend 제한에 맞게 metadata를
제한해야 합니다. Commit properties는 완전한 event store가 아닙니다. 이전 요약 데이터는
계속 읽을 수 있지만 과거 writer가 누락한 attributes는 복구할 수 없습니다.
