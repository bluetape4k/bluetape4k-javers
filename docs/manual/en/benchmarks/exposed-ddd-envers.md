# JaVers + Exposed and Hibernate Envers

The released benchmark compares two audit implementations inside the `javers-exposed-ddd` test module. Its result is useful only when the workload and semantic differences remain attached to the numbers.

## Recorded workload

[`EnversComparisonBenchmarkTest`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt) creates a fresh in-memory H2 database for each implementation and runs three scenarios:

- **insert**: persist forty distinct orders after five warmup inserts;
- **update**: prepare forty orders, then change each status to `PAID` once;
- **audit-query**: query audit information once for each of those forty updated IDs.

The measurements are sequential elapsed wall-clock time collected with `measureNanoTime`. Each scenario reports total milliseconds and milliseconds per operation. Lower is better.

The exact reproduction command is:

```bash
./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

## Recorded environment

The released [JSON artifact](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json) records:

| Field | Value |
|---|---|
| generated timestamp | `2026-05-27T00:00:00Z` |
| Java | `21.0.11` |
| operating system | macOS |
| architecture | `aarch64` |
| database | in-memory H2 |
| warmup iterations | 5 |
| measured iterations | 40 per scenario |
| metric | milliseconds per operation; lower is better |

## Representative values

| Scenario | Hibernate Envers total ms | Envers ms/op | JaVers + Exposed total ms | JaVers + Exposed ms/op |
|---|---:|---:|---:|---:|
| insert | 41.965 | 1.049 | 145.083 | 3.627 |
| update | 55.325 | 1.383 | 113.925 | 2.848 |
| audit-query | 320.414 | 8.010 | 4213.564 | 105.339 |

Envers was faster for every recorded scenario in this run. The largest difference appeared in the audit query. Treat that as a prompt to profile the target query and data shape, especially when audit reads sit on a latency-sensitive path.

## Why this is not an equal-feature contest

The two paths do not perform semantically identical work.

- The Envers insert and update persist one audited JPA entity and its revision rows.
- The JaVers + Exposed path persists the command-side `Order`, creates a JaVers commit and CDO snapshot, and includes domain-event metadata in the commit. The example uses a no-op publisher during this benchmark, so Kafka and Redis are not measured.
- The Envers audit scenario calls `getRevisions`, which returns revision identifiers.
- The JaVers scenario calls `OrderRepository.loadHistory`, which queries the aggregate's snapshot history through the JaVers repository contract.

The result therefore compares the released example paths, not two interchangeable implementations of one normalized storage operation. It cannot isolate how much time belongs to Exposed, JaVers serialization, metadata, table access, Hibernate, or Envers.

## What the result does not prove

This artifact does not prove:

- that Envers is always faster, or that JaVers is unsuitable for the measured operations;
- production PostgreSQL performance or behavior of any other database;
- throughput, tail latency, or contention under concurrent commands and queries;
- steady-state JVM performance across multiple forks, controlled GC, or confidence intervals;
- behavior with large histories, wide object graphs, custom mappings, indexes, caches, or network latency;
- Kafka publication, Redis projection, end-to-end command latency, or recovery cost;
- equivalent audit semantics, storage volume, query results, or operational complexity;
- performance of repository code or benchmark modules added after 0.2.1.

It is a single local documentation run, not a JMH benchmark or release-wide performance claim.

## Use the evidence in a design decision

Begin with the audit question the service must answer. If entity revision tables and revision-ID lookup cover the requirement, benchmark that Envers path with production mappings. If the service needs JaVers object diffs, shadows, commit properties, aggregate-oriented history, and explicit integration with the DDD command path, preserve those semantics in the comparison rather than timing a smaller substitute.

For either choice:

1. move the workload to the target PostgreSQL version and production-like schema;
2. seed realistic history depth and object size;
3. make both query paths return equivalent business information where possible;
4. measure repeated forks, distributions, allocation, database statements, and plans;
5. test concurrent writers and the audit read patterns the service will expose;
6. record recovery and operational costs separately from request latency.

See [benchmark evidence](overview.md), the [released example](../examples/javers-exposed-ddd.md), and [Exposed persistence](../persistence/exposed.md) for the surrounding contracts.
