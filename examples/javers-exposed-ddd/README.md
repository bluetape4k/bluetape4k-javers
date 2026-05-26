# javers-exposed-ddd

English | [한국어](./README.ko.md)

CQRS example for JaVers, Exposed JDBC, Kafka, Redis, and the `javers-ddd`
helpers.

## Flow

```mermaid
sequenceDiagram
    participant Client
    participant Handler as OrderCommandHandler
    participant Store as Exposed order table
    participant Audit as ExposedCdoSnapshotRepository
    participant Kafka as Kafka topic
    participant Consumer as OrderProjectionEventConsumer
    participant Redis as Redis OrderSummary
    participant Query as OrderQueryService

    Client->>Handler: PlaceOrderCommand
    Handler->>Store: insert/update Order
    Handler->>Audit: JaVers commit
    Handler->>Kafka: OrderPlaced
    Consumer->>Kafka: poll event
    Consumer->>Redis: upsert OrderSummary
    Client->>Query: findSummary(orderId)
    Query->>Redis: get summary
    Redis-->>Client: OrderSummary
```

```mermaid
flowchart LR
    command[Command side] --> exposed[(Exposed order table)]
    command --> audit[(JaVers snapshots)]
    command --> kafka[[Kafka order events]]
    kafka --> consumer[Projection consumer]
    consumer --> redis[(Redis read model)]
    query[Read API] --> redis
```

## What This Example Covers

- `Order` aggregate implementing `AggregateRoot<OrderId>`.
- `OrderPlaced` and `OrderMarkedPaid` domain events.
- Exposed-backed source-of-truth order persistence.
- JaVers snapshots persisted through `ExposedCdoSnapshotRepository`.
- Kafka-backed domain event publication.
- Redis-backed `OrderSummary` projection.
- `OrderQueryService` read-side lookup API.
- H2 command handler tests plus Kafka and Redis Testcontainers projection flow.

Benchmark results are handled by the next slice of parent issue #5.

## Run

```bash
./gradlew :javers-exposed-ddd:test
```
