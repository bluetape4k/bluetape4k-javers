# javers-ddd

`javers-ddd` connects source-of-truth aggregate persistence, a JaVers commit, and a domain-event publisher call. It fixes the order in which those contracts are invoked; the adapter still decides when delivery happens and whether the caller observes a delivery failure.

## Dependency and when to choose it

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

Choose this module when aggregates have stable IDs, JaVers history should carry domain-event metadata, and the service can explicitly own recovery between persistence, audit, and publication. Use `javers-core` directly if the service does not need the aggregate repository and publisher contracts.

The central APIs are [`AggregateRoot`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRoot.kt), [`AggregateRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt), [`DomainEvent`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEvent.kt), and [`DomainEventPublisher`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEventPublisher.kt).

## Runnable quick start: aggregate commit metadata

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

For one event, `toJaversProperties()` records `domainEventType`, `aggregateId`, `occurredOn`, and namespaced `event.*` attributes. For multiple events it records only `domainEventCount` and comma-separated `domainEventTypes`; per-event attributes are not retained. This mapping is defined in [`DomainEvent.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEvent.kt).

## Execution and failure semantics

`save` calls subclass `persist`, then `javers.commit`, then `eventPublisher.publishAll`. `publishAll` invokes each publisher in collection order, but actual delivery semantics depend on the implementation. The no-op, function, and composite publishers run immediately and propagate an exception from `publish`. The Spring application-event and Kafka adapters use `publishAfterCommit`, so an active Spring transaction can defer their work until `afterCommit`. The Kafka adapter waits for the `KafkaTemplate.send()` acknowledgement up to `publishTimeout`: without an active transaction, send failure, timeout, or interruption is thrown from `publish`; with an active transaction, rollback sends nothing and transaction completion propagates the same failure. The deferred path therefore fails at transaction completion rather than at the original `save` call, and it is still not a transactional outbox.

If domain persistence fails, no audit or publisher call follows. If the JaVers commit fails, domain state may already exist. An immediate publisher failure can leave domain state and audit in place while later publishers or events have not run. A Kafka acknowledgement failure is observable either from `publish` or from transaction completion, depending on whether `afterCommit` deferral is active; callers still need delivery-failure monitoring and recovery.

`load` checks the source-of-truth repository first and falls back to the latest JaVers shadow. `loadHistory` returns snapshots for the aggregate ID. A shadow is useful for audit reconstruction, but it should not silently replace a required operational database.

The provided no-op, function, and composite publishers are immediate adapters. Spring application events, Spring Kafka, and NATS surfaces require their matching runtime dependencies and have adapter-specific timing. None is a transactional outbox.

## Operations and testing

Record a command/event ID when retries or deduplication matter. Monitor failures separately for domain persistence, JaVers commit, and event publication. Reconciliation should compare aggregate ID, audit version, and external event state rather than repeating the whole command blindly.

```bash
./gradlew :javers-ddd:test
```

The release test [`AggregateRepositoryTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/test/kotlin/io/bluetape4k/javers/ddd/AggregateRepositoryTest.kt) proves saved state, commit metadata, event publication, history, and shadow fallback. Add failure injection between all three save steps.

## Non-goals

- It does not supply aggregate persistence.
- It does not provide an outbox, retry queue, or deduplication store.
- It does not guarantee exactly-once event delivery.
- It does not make domain, audit, and messaging resources atomic.

Continue with [DDD and CQRS](../guides/ddd-and-cqrs.md), [failure contracts](../operations/failure-contracts.md), and the released [Javers Exposed DDD example](../examples/javers-exposed-ddd.md).
