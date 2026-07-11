# JaVers Exposed Benchmark

English | [한국어](README.ko.md)

This module contains bounded `kotlinx-benchmark`/JMH smoke benchmarks for
JaVers Exposed persistence. The benchmark code runs outside normal example
tests so release and CI jobs can validate benchmark drift intentionally.

## Scope

`ExposedCommitMetadataIndexBenchmark` measures optional secondary indexes on
the JaVers Exposed commit metadata table:

- `baseline`: current production schema.
- `author`: benchmark-only `author` index.
- `commit_date`: benchmark-only `commit_date` index.
- `both`: benchmark-only `author` plus `commit_date` indexes.

`EnversComparisonBenchmark` compares the bounded audit workflow paths used by
the Exposed DDD example:

- `envers`: Hibernate Envers entity revisions.
- `javers_in_memory`: JaVers core diff/query cost before persistence adapters.
- `javers_exposed_repository`: JaVers Exposed snapshot repository cost.
- `javers_exposed_ddd`: source-table persistence plus aggregate repository orchestration.

The benchmark creates temporary PostgreSQL tables per trial and drops them at
tear-down. Production JaVers Exposed schema defaults are not changed by this
module.

## Run

Smoke run used by CI and full Nightly:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Envers comparison smoke run:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Full local benchmark target:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Run benchmark tasks serially. The module uses PostgreSQL Testcontainers,
HikariCP, `bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`, and
`bluetape4k-exposed-jdbc-tests`. Containers are non-reusable by default in
local runs and CI. A developer may explicitly opt in to local reuse by enabling
Testcontainers reuse in `~/.testcontainers.properties` and adding
`-Dbluetape4k.testcontainers.reuse=true` to `JAVA_TOOL_OPTIONS`. The opt-in is ignored
when `CI` or `GITHUB_ACTIONS=true` is present. A reusable PostgreSQL container is
left running across benchmark JVM exits; a non-reusable container is closed when
its benchmark JVM exits.

## Result Snapshot

The committed snapshot was produced with one warmup iteration and one measured
iteration on JDK 21.0.11. Scores are throughput in operations per second;
higher is better.

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](../../docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](../../docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s | Decision signal |
|---|---:|---:|---:|---|
| Baseline | 481.4 | 917.5 | 916.5 | Stable reference for the current production schema. |
| Author index | 488.6 | 907.1 | 904.7 | No author-query benefit in this smoke run. |
| `commit_date` index | 499.3 | 931.2 | 923.2 | Slight read throughput gain, but bounded evidence only. |
| Author + `commit_date` indexes | 518.6 | 945.9 | 873.8 | Best author-query throughput, but weaker date-range throughput. |

The results are local smoke evidence, not a release-wide performance claim. A
broader workload must justify production index defaults before the schema is
changed.
