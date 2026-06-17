# examples-javers-exposed-ddd

English | [한국어](./README.ko.md)

CQRS example for JaVers, Exposed JDBC, Kafka, Redis, and the `javers-ddd`
helpers.

## Flow

The example has two separate responsibilities. The command side owns the order
row and JaVers audit snapshot. The event/read side publishes order domain events
to Kafka and projects them into Redis for query reads.

![javers-exposed-ddd CQRS flow](../../docs/images/readme-diagrams/examples-javers-exposed-ddd-cqrs-flow-01.png)

The command handler saves aggregate state before the Kafka event is projected.
The read API intentionally reads the Redis projection only; it does not query
`OrdersTable` or JaVers snapshots.

![javers-exposed-ddd command and projection sequence](../../docs/images/readme-diagrams/examples-javers-exposed-ddd-sequence-01.png)

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

### Commit Metadata Index Evaluation

The commit metadata index benchmark compares the same JaVers Exposed SQL
pushdown path with benchmark-only `author` and `commit_date` indexes on
PostgreSQL 18-alpine via Testcontainers and HikariCP. It lives in the dedicated
`kotlinx-benchmark` module and reuses `bluetape4k-jdbc`,
`bluetape4k-exposed-jdbc`, and `bluetape4k-exposed-jdbc-tests`. Production
schema defaults remain unchanged by this benchmark.

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](../../docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](../../docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s |
|---|---:|---:|---:|
| Baseline | 481.4 | 917.5 | 916.5 |
| Author index | 488.6 | 907.1 | 904.7 |
| `commit_date` index | 499.3 | 931.2 | 923.2 |
| Author + `commit_date` indexes | 518.6 | 945.9 | 873.8 |

## Run

```bash
./gradlew :examples-javers-exposed-ddd:test
```
