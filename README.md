# bluetape4k-javers

[![CI](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

English | [한국어](./README.ko.md)

![Bluetape4k JaVers audit workbench](./docs/assets/javers-workbench.png)

Kotlin/JVM integrations for [JaVers](https://javers.org) object auditing and
diffing, with Exposed JDBC, Redis, and Kafka persistence options for CDO snapshots and
event-sourced change streams.

## Project Purpose

`bluetape4k-javers` extends JaVers beyond its built-in in-memory, MongoDB, and
JDBC storage choices. It focuses on Kotlin-friendly audit/diff infrastructure
for services that need cache-backed reads, Redis persistence, Kafka event
streams, and an Exposed-backed repository layer.

Use this repository when an application wants JaVers' object diff model together
with the bluetape4k stack: Kotlin-first helpers, Exposed JDBC persistence,
cache/stream adapters, and a CQRS example that shows how audit commits and
domain events fit together.

## What It Provides

- **JaVers core helpers** — extensions, codecs, and cache-backed repository
  building blocks.
- **Exposed JDBC persistence** — Exposed schema and repository for SQL-backed
  JaVers CDO snapshots.
- **DDD helpers** — aggregate root, domain event, repository, and publisher
  adapters for JaVers-backed audit workflows.
- **CQRS command-side example** — Exposed + JaVers + DDD helper order command
  flow under `examples/javers-exposed-ddd`.
- **Ktor REST example** — explicit Ktor wiring for Exposed command persistence
  and JaVers audit history under `examples/javers-ktor`.
- **Spring Boot 4 REST example** — explicit Spring Boot 4 wiring for Exposed
  command persistence and JaVers audit history under `examples/javers-spring-boot4`.
- **Redis persistence** — Lettuce and Redisson based snapshot storage paths.
- **Kafka persistence** — event-stream backed CDO snapshot persistence.
- **BOM support** — `bluetape4k-javers-bom` for aligned consumer dependency
  versions.
- **Implementation backlog** — documented phase chain for Exposed persistence,
  DDD helpers, and CQRS/Event Sourcing examples.

## Persistence Options

![JaVers persistence options relationship diagram](./docs/assets/javers-persistence-options.png)

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Bluetape4k JaVers overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Bluetape4k JaVers module composition chart](docs/images/readme-charts/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## Architecture

![javers Architecture diagram](docs/images/readme-diagrams/bluetape4k-javers-architecture-01.png)

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| `javers-core` | `io.github.bluetape4k.javers:javers-core` | JaVers extensions, codecs, cache-backed and composite repositories |
| `javers-ddd` | `io.github.bluetape4k.javers:javers-ddd` | DDD aggregate/domain-event helpers for JaVers audit workflows |
| `javers-exposed` | `io.github.bluetape4k.javers:javers-exposed` | Exposed JDBC CDO snapshot persistence |
| `examples-javers-exposed-ddd` | example module | CQRS command-side example using Exposed persistence and JaVers DDD helpers |
| `examples-javers-ktor` | example module | Ktor REST example using explicit Exposed and JaVers wiring |
| `examples-javers-spring-boot4` | example module | Spring Boot 4 REST example using explicit Exposed and JaVers wiring |
| `javers-persistence-redis` | `io.github.bluetape4k.javers:javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `javers-persistence-kafka` | `io.github.bluetape4k.javers:javers-persistence-kafka` | Kafka-backed CDO snapshot persistence (write-only event stream; reads always return empty) |
| `bluetape4k-javers-bom` | `io.github.bluetape4k.javers:bluetape4k-javers-bom` | Consumer BOM for aligned JaVers artifacts |

## Dependency Setup

Use the BOM when consuming more than one module:

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:0.2.1"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

Add only the persistence adapters that the application actually uses. Kafka,
Redis, and Exposed modules intentionally serve different storage and eventing
roles.

## Quick Start

```kotlin
val snapshotRepository = ExposedCdoSnapshotRepository(database)
snapshotRepository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(snapshotRepository)
    .registerEntity(Order::class.java)
    .build()
```

For DDD command handling, write the aggregate to the source-of-truth store,
commit it to JaVers, then publish a domain event. The
`examples/javers-exposed-ddd` module shows this path with Kafka events and a
Redis read model.

The `examples/javers-spring-boot4` module shows the same explicit JaVers +
Exposed command persistence behind Spring Boot 4 REST endpoints, without relying
on future auto-configuration.

The `examples/javers-ktor` module shows the same current feature set behind Ktor
REST endpoints for non-Spring users, reusing bluetape4k Ktor JSON and health
helpers.

## Requirements

- JDK 21+
- Kotlin 2.3+
- JaVers 7.11.0

## Build

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-ddd:test
./gradlew :javers-exposed:test
./gradlew :examples-javers-exposed-ddd:test
./gradlew :examples-javers-ktor:test
./gradlew :examples-javers-spring-boot4:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Benchmark Snapshot

The comparison below is a bounded documentation benchmark, not a release-wide
performance claim. It was generated by
`./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
with 5 warmup operations and 40 measured operations per scenario. Lower
milliseconds per operation is better.

Environment: PostgreSQL 18-alpine via Testcontainers, HikariCP, JDK 21.0.11,
macOS aarch64. Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json`](docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json).

![JaVers Exposed DDD benchmark comparison](docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png)

| Implementation | Insert ms/op | Update ms/op | Audit-query ms/op |
|---|---:|---:|---:|
| Hibernate Envers | 4.486 | 6.917 | 12.483 |
| JaVers in-memory | 0.510 | 0.978 | 12.559 |
| JaVers + Exposed repository | 8.499 | 5.945 | 0.763 |
| JaVers + Exposed DDD path | 6.397 | 7.257 | 0.704 |

The Exposed lanes include PostgreSQL round trips through HikariCP. The DDD path
also includes source-of-truth order persistence and aggregate repository
orchestration, so compare it as an end-to-end example path rather than as a pure
snapshot repository cost.

### Commit Metadata Index Evaluation

Issue #188 evaluates optional secondary indexes on the JaVers Exposed commit
metadata table. The benchmark below is a dedicated `kotlinx-benchmark`/JMH
harness that uses PostgreSQL 18-alpine via Testcontainers, HikariCP,
`bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`, and
`bluetape4k-exposed-jdbc-tests`. Scores are throughput in operations per second;
higher values are better.

Command:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s | Decision signal |
|---|---:|---:|---:|---|
| Baseline | 481.4 | 917.5 | 916.5 | Stable reference for the current production schema. |
| Author index | 488.6 | 907.1 | 904.7 | No author-query benefit in this smoke run. |
| `commit_date` index | 499.3 | 931.2 | 923.2 | Slight read throughput gain, but bounded evidence only. |
| Author + `commit_date` indexes | 518.6 | 945.9 | 873.8 | Best author-query throughput, but weaker date-range throughput. |

The candidate indexes are created only inside the benchmark schema. Production
JaVers Exposed defaults remain unchanged because this short smoke run is mixed;
a broader workload benchmark must justify the extra DDL and write-amplification
cost before default indexes are added.

### Combined Interpretation

The two benchmark families answer different questions. The Envers comparison
measures broader audit workflows in milliseconds per operation, while the
commit metadata benchmark measures a narrower JaVers Exposed SQL pushdown path
in throughput. The table below converts the metadata benchmark to approximate
milliseconds per operation with `1000 / opsPerSecond` so insert, update, and
read-side results can be read on one latency scale. The commit metadata
benchmark does not include an update scenario because JaVers commit metadata is
append-only in this repository path.

![JaVers Exposed combined benchmark overview](docs/images/readme-charts/javers-exposed-combined-benchmark-overview-01.png)

| Path | Scope | Insert ms/op | Update ms/op | Read ms/op | Interpretation |
|---|---|---:|---:|---:|---|
| Hibernate Envers | Entity revision audit path | 4.486 | 6.917 | 12.483 audit-query | Baseline JPA audit path for the example domain. |
| JaVers in-memory | Core JaVers diff/query path | 0.510 | 0.978 | 12.559 audit-query | Fast writes and updates, but in-memory audit query is not the Exposed SQL path. |
| JaVers + Exposed repository | Snapshot repository path | 8.499 | 5.945 | 0.763 audit-query | Higher write cost, but repository read path is the fastest full audit-query lane here. |
| JaVers + Exposed DDD path | End-to-end example path | 6.397 | 7.257 | 0.704 audit-query | Adds source-table and aggregate orchestration, but keeps fast audit reads. |
| JaVers Exposed metadata baseline | Commit metadata author/date filters | 2.077 | Not measured | 1.090 author / 1.091 date-range | Current production schema is already near the Exposed audit-query order of magnitude for metadata reads. |
| Best metadata-index smoke variants | Benchmark-only candidate indexes | 1.928 | Not measured | 1.057 author / 1.083 date-range | Insert is slightly faster in the smoke run, but read gains remain small and mixed. |

Taken together, the Exposed repository remains the right read-side direction
against Envers for this workload, but it pays more on insert/update than the
in-memory path. The commit metadata index decision should stay conservative:
the indexed smoke variants improve insert a little and only some read filters,
while update is outside that append-only benchmark scope.

## References

- [JaVers](https://javers.org)
- [JaVers Feature Overview](https://javers.org/features)
- [JaVers VS Envers Comparison](https://javers.org/blog/2017/12/javers-vs-envers-comparision.html)
- [Using JaVers for Data Model Auditing in Spring Data](https://www.baeldung.com/spring-data-javers-audit)
