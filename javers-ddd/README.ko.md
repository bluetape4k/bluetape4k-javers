# Module bluetape4k-javers-ddd

[English](./README.md) | 한국어

`javers-ddd`는 aggregate root를 이미 저장하고 있는 서비스가 JaVers audit
history와 트랜잭션 경계를 인식하는 domain-event 발행을 함께 붙일 수 있도록 돕는
작은 DDD helper layer입니다. 이 모듈은 source-of-truth repository를 대체하지 않습니다. Repository
subclass가 여전히 persistence와 lookup을 책임지고, 이 모듈은 그 workflow 주변에
JaVers commit과 event publisher 단계를 추가합니다.

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
val history = repository.loadHistory(1)
```

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
