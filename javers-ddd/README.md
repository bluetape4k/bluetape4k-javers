# Module bluetape4k-javers-ddd

English | [한국어](./README.ko.md)

`javers-ddd` is a small DDD helper layer for services that already persist
aggregate roots and want JaVers audit history plus transaction-aware domain-event
publication. It does not replace your source-of-truth repository. Your
repository subclass still owns persistence and lookup; this module adds the
JaVers commit and event-publisher step around that workflow.


## Optional auditing adapter for external aggregates

`AggregateAuditAdapter` audits the original Exposed aggregate through extractor functions. Consumers do not
implement a second marker, and javers-ddd gains no required Exposed dependency. This example is for consumers
already using Exposed; `order`, `persist`, and `eventPublisher` belong to the application.

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

The mapper preserves the original ID, occurrence time, type, and required attributes. This example assumes
events without additional attributes. Multiple events reuse the existing collection metadata encoder.

Call capture, audit inside the source transaction, confirm its successful return, publish, then complete with
COMMITTED. ROLLED_BACK/UNKNOWN terminate without clearing the buffer. Whether the JaVers backend joins the source
transaction depends on its configuration; distributed atomicity is not guaranteed. A fresh registration after
publication failure can deliver duplicates, so consumers must be idempotent.

Events must be deeply immutable; one caller keeps the aggregate and buffer stable until completion. Callbacks
must not mutate state, and clear must atomically remove all events or fail without changing the buffer. Reference
order checks cannot detect internal object mutations, and partial clear recovery is unsupported. Captured references
remain available through `events`. All callbacks execute synchronously with O(N) capture/mapping costs. The caller
sets execution thread, I/O timeouts, event-count and retry limits. No automatic retry, transaction, outbox, or dispatcher
is created.

## Class Diagram

![javers-ddd class diagram](../docs/images/readme-diagrams/javers-ddd-class-diagram-01.png)

## Save Flow

![javers-ddd save flow](../docs/images/readme-diagrams/javers-ddd-save-flow-01.png)

## Core Responsibilities

- `AggregateRoot<ID>` marks audited aggregate roots and exposes a stable `id`.
- `DomainEvent` describes an event emitted by an aggregate and maps event
  metadata into JaVers commit properties.
- `AggregateRepository<T, ID>` saves the aggregate through subclass persistence,
  commits the saved state to JaVers, and publishes events after both steps
  succeed.
- `DomainEventPublisher` is a synchronous fail-fast publisher contract.
- Built-in publishers cover no-op, Kotlin function, composite fan-out, Spring
  application events, Spring Kafka, and NATS.

## Usage

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
        // Persist with Exposed, Spring Data, or another source-of-truth store.
        return aggregate
    }

    override fun findById(id: Long): Order? = null
}

val repository = OrderRepository(javers)
repository.save(Order(1, "PLACED"), "system", OrderPlaced(1))
val history = repository.loadHistory(1, limit = 20)
```

`loadHistory` returns snapshots newest-first and forwards the positive `limit`
to the JaVers query. Omitting it keeps the default limit of 100; use the
bounded overload when an API request exposes a smaller history window.

Annotate the aggregate id with JaVers `@Id`, then register aggregate types as
JaVers entities when building `Javers`:

```kotlin
val javers = JaversBuilder.javers()
    .registerJaversRepository(exposedSnapshotRepository)
    .registerEntity(Order::class.java)
    .build()
```

## Publisher Options

Use the smallest publisher that matches the service boundary:

| Publisher | Use when |
|---|---|
| `NoopDomainEventPublisher` | The service only needs JaVers commit metadata. |
| `FunctionDomainEventPublisher` | Tests or small applications can supply a lambda. |
| `CompositeDomainEventPublisher` | Multiple local publishers should run in order. |
| `SpringApplicationEventDomainEventPublisher` | Spring listeners consume events in-process. |
| `KafkaDomainEventPublisher` | Spring Kafka sends domain events to a topic. |
| `NatsDomainEventPublisher` | A NATS connection publishes serialized event payloads. |

Spring and Kafka publishers defer publication to `afterCommit` when Spring
transaction synchronization is active. Otherwise they publish immediately. Kafka
waits for the broker acknowledgement up to `publishTimeout`; send failure,
timeout, and interruption are propagated from the immediate call or transaction
completion, while rollback sends nothing. NATS publishes synchronously from the
repository call using the subject resolver and serializer supplied by the consumer.

## Delivery Semantics

`AggregateRepository` publishes events after source persistence and the JaVers
commit have succeeded. Local publishers are immediate, while Spring and Kafka
publishers may defer until `afterCommit`; Kafka acknowledgement failures remain
observable at the documented completion point. This is not a durable outbox
implementation. Use a transactional outbox when exactly-once external delivery,
replay, or cross-service recovery is required.

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

Spring, Kafka, and NATS adapters are optional surfaces. Add the matching runtime
dependency only when using the adapter.

## Build

```bash
./gradlew :javers-ddd:test
```
