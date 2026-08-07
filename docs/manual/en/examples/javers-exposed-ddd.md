# JaVers + Exposed DDD order flow

This released example follows one order through a command-side Exposed table, a JaVers audit repository, Kafka domain events, and a Redis read model. Its value is not the amount of code. It makes the boundaries between source-of-truth state, audit history, event delivery, and query projection visible enough to test.

## The problem the example solves

`PlaceOrderCommand` and `MarkOrderPaidCommand` change an `Order` aggregate. A useful implementation must answer four different questions:

1. What is the current order state?
2. Who changed it, and what did JaVers record?
3. Which domain event left the command side?
4. What can the query side read without loading the aggregate?

The example assigns those jobs to different stores. Exposed persists the current `Order`; `ExposedCdoSnapshotRepository` persists JaVers commits and snapshots; Kafka carries `OrderPlaced` and `OrderMarkedPaid`; Redis stores `OrderSummary`. Redis is a projection, not the source of truth, and the JaVers tables are an audit store, not the command-side order table.

## Released fixture boundary

Run the example from the repository root with JDK 21 and a Docker-compatible container runtime.

```bash
./gradlew :javers-exposed-ddd:test
```

The released tests do **not** start a PostgreSQL server. `OrderCommandHandlerTest` and `OrderProjectionFlowTest` use an in-memory H2 database with `MODE=PostgreSQL`. The projection test starts Kafka and Redis with the Projects Testcontainers launchers. Therefore the test boundary is:

| Responsibility | Released fixture | What it proves |
|---|---|---|
| order and JaVers tables | H2, PostgreSQL compatibility mode | command, snapshot, and history behavior in the example schema |
| domain-event transport | Kafka Testcontainer | encoded events can be published and consumed by order key |
| query projection | Redis Testcontainer | consumed events produce a readable `OrderSummary` |

Use a real PostgreSQL Testcontainer before adopting the schema or transaction assumptions in production. H2 compatibility mode does not prove PostgreSQL DDL, locking, isolation, query-plan, or driver behavior. The [Exposed transaction-ownership guide](https://bluetape4k.github.io/manual/bluetape4k-exposed/1.11/modules/bluetape4k-exposed-jdbc/transaction-ownership/) explains application repository boundaries, while the [Projects manual](https://bluetape4k.github.io/manual/bluetape4k-projects/) covers PostgreSQL, Kafka, Redis, and Testcontainers facilities.

## Follow the command path

The smallest useful reading sequence is:

1. [`Order.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/Order.kt) defines the aggregate invariant and `PLACED` to `PAID` transition.
2. [`OrderCommand.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/OrderCommand.kt) separates request intent from stored state.
3. [`OrderCommandHandler.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/service/OrderCommandHandler.kt) creates the aggregate or loads and changes it.
4. [`OrderRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/persistence/OrderRepository.kt) stores `example_order` through Exposed, then delegates the JaVers commit and event publication to `AggregateRepository`.
5. [`OrderCommandHandlerTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/OrderCommandHandlerTest.kt) states the expected command-side result.

For `PlaceOrderCommand(order-1)` with two `12.50` items, the test expects:

- current status `PLACED` and total `25.00` in the source-of-truth order;
- one JaVers snapshot whose `domainEventType` is `OrderPlaced`;
- one published `OrderPlaced` with customer, total, aggregate ID, and fixed occurrence time.

After `MarkOrderPaidCommand`, another test expects current status `PAID`, two audit snapshots, and the publication order `OrderPlaced`, then `OrderMarkedPaid`.

Run only these command-side tests when changing aggregate or audit behavior:

```bash
./gradlew :javers-exposed-ddd:test --tests '*OrderCommandHandlerTest*'
```

## Follow the projection path

Continue from the domain event to the query model:

1. [`OrderEvents.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/OrderEvents.kt) defines audit attributes and event payload fields.
2. [`OrderDomainEventJsonCodec.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderDomainEventJsonCodec.kt) fixes the example-local JSON shape.
3. [`OrderKafkaEventPublisher.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderKafkaEventPublisher.kt) sends synchronously and uses the order ID as the record key.
4. [`OrderProjectionEventConsumer.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderProjectionEventConsumer.kt) polls records and applies them in poll order.
5. [`RedisOrderSummaryProjection.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/projection/RedisOrderSummaryProjection.kt) writes one JSON document per order.
6. [`OrderQueryService.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/service/OrderQueryService.kt) reads only Redis.

Run the released end-to-end projection test:

```bash
./gradlew :javers-exposed-ddd:test --tests '*OrderProjectionFlowTest*'
```

The test places two `15.00` items and polls Kafka until Redis contains an `OrderSummary` with customer `customer-projection-1`, total `30.00`, and status `PLACED`. It then marks the order paid and waits for the same Redis key to show status `PAID` and the fixed update timestamp. The polling loop is an assertion aid, not an operational consumer loop.

## Learn the stack in layers

Use the example as a sequence rather than reading every class at once:

1. [javers-core](../modules/javers-core.md): commit an object and query snapshots without persistence integration.
2. [javers-exposed](../modules/javers-exposed.md): replace the in-memory JaVers repository with Exposed-backed commit and snapshot tables.
3. [javers-ddd](../modules/javers-ddd.md): add aggregate persistence, audit metadata, and a synchronous `DomainEventPublisher` after each transition.
4. Kafka event: serialize `OrderPlaced` or `OrderMarkedPaid` and publish with the stable order ID key.
5. Redis projection: consume the event into `OrderSummary`, then query Redis independently from the command-side tables.

This order keeps the responsibility of each layer visible. It also makes failures easier to inject and reconcile. See [DDD and CQRS](../guides/ddd-and-cqrs.md), [failure contracts](../operations/failure-contracts.md), and the [benchmark interpretation](../benchmarks/exposed-ddd-envers.md).

## Production gaps and failure boundaries

The release example deliberately leaves production coordination to the application:

- `OrderRepository.persist`, the JaVers commit, and Kafka publication are three sequential operations, not one transaction.
- A JaVers failure can leave the order row committed without a matching audit snapshot.
- A Kafka failure can leave both order state and audit history committed without an event.
- There is no transactional outbox, inbox, retry queue, dead-letter policy, event ID, deduplication store, or replay checkpoint.
- Kafka ordering is expected only for records routed to the same partition by order ID. Topic and producer configuration still matter.
- `OrderProjectionEventConsumer` does not manage offsets, retries, poison records, rebalances, or graceful shutdown for a service.
- `OrderMarkedPaid` requires an existing Redis summary. A missing, duplicated, or out-of-order event can fail or regress the projection.
- The JSON codec is example-local and has no schema version or compatibility policy.
- Redis stores the latest view only. It is not a substitute for the order table or JaVers history.
- The tests do not cover real PostgreSQL, process restarts, broker outages, concurrent commands, or projection rebuilds.

For production, introduce an explicit outbox and consumer idempotency policy, version event schemas, persist replay progress, and provide a projection rebuild path from a durable event or audit source. Instrument the three command-side steps separately.

## Exercises that expose the boundaries

1. Replace H2 with a PostgreSQL Testcontainer and verify schema creation, timestamps, and concurrent updates.
2. Fail the JaVers repository after `example_order` commits; record how reconciliation finds the missing audit version.
3. Fail Kafka publication, add an outbox row in the same database transaction as the order, and publish it later.
4. Add an event ID and make Redis projection updates idempotent under duplicate delivery.
5. Deliver `OrderMarkedPaid` before `OrderPlaced`; decide whether to retry, park, or rebuild the summary.
6. Delete the Redis key and rebuild it without reading the command-side table directly.
7. Version the event JSON and keep old payloads readable in a codec compatibility test.

The full released dependency set is pinned in [`build.gradle.kts`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/build.gradle.kts). Keep the example's claims at this release boundary when comparing it with later repository code.
