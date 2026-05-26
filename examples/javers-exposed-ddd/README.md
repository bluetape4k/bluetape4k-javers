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

## Benchmark

The Envers comparison benchmark is a local documentation benchmark, not a
release-wide performance claim. It runs on H2 and measures the simple example
shape used in this module. Lower milliseconds per operation is better.

Command:

```bash
./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Environment and raw results are stored in
[`docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json`](../../docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json).

| Scenario | Hibernate Envers ms/op | JaVers + Exposed ms/op |
|---|---:|---:|
| insert | 1.049 | 3.627 |
| update | 1.383 | 2.848 |
| audit-query | 8.010 | 105.339 |

On this H2 run Envers is faster for the narrow persistence benchmark. The
JaVers + Exposed example is still useful when the application needs explicit
aggregate commits, commit metadata, domain-event integration, and a CQRS
projection path rather than only entity revision tables.

## Run

```bash
./gradlew :javers-exposed-ddd:test
```
