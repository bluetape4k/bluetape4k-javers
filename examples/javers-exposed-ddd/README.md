# examples-javers-exposed-ddd

English | [한국어](./README.ko.md)

CQRS example for JaVers, Exposed JDBC, Kafka, Redis, and the `javers-ddd`
helpers.

## Flow

![javers-exposed-ddd command and projection sequence](docs/images/readme-diagrams/javers-exposed-ddd-sequence-01.png)

![javers-exposed-ddd CQRS flow](docs/images/readme-diagrams/javers-exposed-ddd-cqrs-flow-01.png)

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
./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Environment and raw results are stored in
[`docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json`](../../docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json).

![JaVers Exposed DDD benchmark comparison](../../docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png)

| Lane | insert ms/op | update ms/op | audit-query ms/op | Notes |
|---|---:|---:|---:|---|
| Hibernate Envers | 1.084 | 1.528 | 10.236 | Hibernate entity revision tables; audit query loads audited entity revisions. |
| JaVers in-memory | 0.490 | 0.939 | 12.208 | Approximate JaVers core diff/query path before persistence adapters. |
| JaVers + Exposed repository | 3.039 | 1.765 | 0.309 | Snapshot repository persistence and query path without the example source table. |
| JaVers + Exposed DDD path | 2.095 | 3.002 | 0.313 | End-to-end example path including `OrdersTable` and aggregate repository orchestration. |

The previous JaVers + Exposed audit-query outlier is not reproduced by this
fresh run. The benchmark still remains a bounded H2 documentation benchmark:
use the table to understand the example's cost shape, not as a release-wide
performance guarantee.

## Run

```bash
./gradlew :examples-javers-exposed-ddd:test
```
