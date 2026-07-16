# javers-ddd

`javers-ddd`는 도메인의 원본 저장, JaVers 커밋, 도메인 이벤트 발행기 호출 순서를 고정합니다. 실제 전달 시점과 실패가 호출자에게 전파되는지는 발행기 구현에 따라 달라집니다.

## 의존성과 선택 기준

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

애그리거트 ID가 안정적이고, JaVers 이력에 도메인 이벤트 메타데이터를 남기며, 저장·감사·발행 사이의 복구를 서비스가 직접 설계할 때 선택합니다. 애그리거트 저장소와 발행자 계약이 필요 없다면 `javers-core`를 바로 쓰는 편이 단순합니다.

핵심 API는 [`AggregateRoot`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRoot.kt), [`AggregateRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt), [`DomainEvent`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEvent.kt), [`DomainEventPublisher`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEventPublisher.kt)입니다.

## 바로 실행하는 예제: 애그리거트 커밋 메타데이터

```kotlin
import io.bluetape4k.javers.ddd.*
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import java.time.Instant

data class Order(
    @Id override val id: Long,
    var status: String,
) : AggregateRoot<Long>

data class OrderPlaced(
    override val aggregateId: Long,
    override val occurredOn: Instant = Instant.now(),
    override val attributes: Map<String, String> = mapOf("channel" to "web"),
) : DomainEvent

class OrderRepository(javers: Javers) :
    AggregateRepository<Order, Long>(Order::class.java, javers) {
    private val rows = mutableMapOf<Long, Order>()
    override fun persist(aggregate: Order) = aggregate.copy().also { rows[it.id] = it }
    override fun findById(id: Long) = rows[id]?.copy()
}

val javers = JaversBuilder.javers().registerEntity(Order::class.java).build()
val repository = OrderRepository(javers)
repository.save(Order(1, "PLACED"), "order-service", OrderPlaced(1))

val snapshot = repository.loadHistory(1).single()
check(snapshot.commitMetadata.properties["domainEventType"] == OrderPlaced::class.qualifiedName)
check(snapshot.commitMetadata.properties["event.channel"] == "web")
```

이벤트가 하나면 `toJaversProperties()`가 `domainEventType`, `aggregateId`, `occurredOn`, `event.*` 속성을 커밋에 넣습니다. 여러 이벤트를 한 번에 넘기면 `domainEventCount`와 쉼표로 연결한 `domainEventTypes`만 남고 이벤트별 속성은 저장하지 않습니다. 정확한 변환은 [`DomainEvent.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEvent.kt)에 있습니다.

## 실행 순서와 실패 경계

`save`는 하위 클래스의 `persist`, `javers.commit`, `eventPublisher.publishAll` 순으로 호출합니다. `publishAll`은 컬렉션 순서대로 발행기를 부르지만, 실제 전달 방식은 구현마다 다릅니다. 아무 일도 하지 않는 발행기, 함수형 발행기, 복합 발행기는 즉시 실행되며 `publish`에서 난 예외를 호출자에게 전달합니다. Spring 애플리케이션 이벤트와 Kafka 발행기는 `publishAfterCommit`을 사용하므로 Spring 트랜잭션이 활성화돼 있으면 `afterCommit`까지 실행을 미룹니다. Kafka 발행기는 `KafkaTemplate.send()`가 돌려주는 `Future`를 기다리지 않으므로 브로커 전송 실패가 `save` 호출에 전달된다고 보장할 수 없습니다.

도메인 저장이 실패하면 감사와 발행기 호출을 시작하지 않습니다. JaVers 커밋이 실패하면 도메인 상태만 남을 수 있습니다. 즉시 실행하는 발행기가 실패하면 도메인 상태와 감사 이력은 있지만 일부 발행자나 뒤 이벤트는 실행되지 않을 수 있습니다. 실행을 미루거나 결과를 기다리지 않는 발행기는 전달 실패를 별도로 관측하고 복구해야 합니다.

`load`는 먼저 원본 저장소를 찾고, 없을 때 최신 JaVers 섀도로 복원합니다. `loadHistory`는 애그리거트 ID의 스냅샷을 읽습니다. 섀도는 감사 복원에는 유용하지만 운영 DB가 반드시 있어야 하는 경로를 조용히 대신해서는 안 됩니다.

기본 제공하는 무동작 발행기, 함수형 발행기, 복합 발행기는 즉시 호출하는 어댑터입니다. Spring 애플리케이션 이벤트, Spring Kafka, NATS 어댑터를 쓰려면 해당 실행 의존성을 추가해야 하며, 전달 시점도 구현별로 확인해야 합니다. 어느 것도 트랜잭셔널 아웃박스가 아닙니다.

## 운영과 테스트

재시도나 중복 제거가 필요하면 명령 ID와 이벤트 ID를 따로 기록합니다. 도메인 저장, JaVers 커밋, 이벤트 발행 실패를 각각 관측하세요. 복구할 때 전체 명령을 무작정 다시 실행하지 말고 애그리거트 ID, 감사 버전, 외부 이벤트 상태를 비교해야 합니다.

```bash
./gradlew :javers-ddd:test
```

릴리스의 [`AggregateRepositoryTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/test/kotlin/io/bluetape4k/javers/ddd/AggregateRepositoryTest.kt)는 저장 결과, 커밋 메타데이터, 이벤트 발행, 이력, 섀도 대체 경로를 검증합니다. 애플리케이션에서는 저장 단계 세 곳 사이에 실패를 주입하는 테스트를 더하세요.

## 하지 않는 일

- 애그리거트 영속 저장을 구현하지 않습니다.
- 아웃박스, 재시도 큐, 중복 제거 저장소를 제공하지 않습니다.
- 이벤트 exactly-once 전달을 보장하지 않습니다.
- 도메인 DB, 감사 저장소, 메시징을 하나의 원자 작업으로 묶지 않습니다.

이어서 [DDD와 CQRS](../guides/ddd-and-cqrs.md), [실패 계약](../operations/failure-contracts.md), 릴리스의 [Javers Exposed DDD 예제](../examples/javers-exposed-ddd.md)를 읽어 보세요.
