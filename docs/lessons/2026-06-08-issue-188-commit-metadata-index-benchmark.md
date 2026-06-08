# Issue #188 commit metadata index benchmark

## Context

Issue #188 asked whether the JaVers Exposed commit metadata table should add
`author` and `commit_date` secondary indexes for SQL pushdown queries.

The first attempt used a JUnit-style documentation benchmark, but that was not a
strong enough basis for a production DDL decision. The benchmark was moved to a
dedicated `benchmark/javers-exposed-benchmark` module using `kotlinx-benchmark`
and JMH.

## Decision

Keep the production JaVers Exposed schema unchanged for now. Candidate indexes
are created only inside benchmark trial tables.

The smoke run used PostgreSQL 18-alpine via Testcontainers, HikariCP,
`bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`, and
`bluetape4k-exposed-jdbc-tests`.

## Outcome

The latest smoke run showed:

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s |
|---|---:|---:|---:|
| Baseline | 481.4 | 917.5 | 916.5 |
| Author index | 488.6 | 907.1 | 904.7 |
| `commit_date` index | 499.3 | 931.2 | 923.2 |
| Author + `commit_date` indexes | 518.6 | 945.9 | 873.8 |

This does not justify adding default read-pushdown indexes yet. The result is
mixed: indexed variants improved some smoke scores, but the combined index
weakened date-range throughput and the run is too short to define a production
DDL policy.

## Verification

- `./gradlew :benchmark-javers-exposed-benchmark:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `xmllint --noout docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg`
- `rsvg-convert docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg -o docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png`

## Future Rule

For JaVers Exposed DDL performance decisions, use a dedicated benchmark module
with PostgreSQL + HikariCP evidence. Do not use JUnit documentation benchmarks
as the sole evidence for production index changes.
