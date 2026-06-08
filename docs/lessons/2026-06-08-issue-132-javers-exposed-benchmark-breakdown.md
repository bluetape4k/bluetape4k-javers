# Issue #132 - JaVers Exposed Benchmark Breakdown

## Context

Issue #90 added a local Envers comparison benchmark, but the documented JaVers
+ Exposed audit-query result showed a large outlier. Issue #132 asked to
separate JaVers core cost from Exposed persistence/query cost and explain the
result with a table and chart. Follow-up review also found that the benchmark
should run on PostgreSQL with HikariCP and that JaVers Exposed tables did not
need surrogate `LongIdTable` ids.

## Decision

Keep this as a documentation benchmark instead of a production optimization
claim. Use PostgreSQL Testcontainers + HikariCP and four lanes:

- `Hibernate Envers`
- `JaVers in-memory`
- `JaVers + Exposed repository`
- `JaVers + Exposed DDD path`

The repository-only lane isolates snapshot repository behavior. The DDD path
lane remains the real example path and includes `OrdersTable`, aggregate
repository orchestration, and event-aware save behavior.

Use natural primary keys for the Exposed JaVers tables:

- `javers_commit`: `commit_id`
- `javers_snapshot`: `(global_id, version)`

This removes the surrogate id primary key and the duplicate unique
`(global_id, version)` index. Do not use `(global_id, commit_id)` as the
snapshot key; JaVers snapshot identity is object id plus object version, while a
commit id can group multiple snapshots.

## Outcome

The PostgreSQL + HikariCP run no longer reproduced the old JaVers + Exposed
audit-query outlier. README English/Korean now show the raw JSON link, PNG
chart, result table, environment, and caveat that the benchmark is bounded to
documentation use.

## Verification

- `./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 1 test, PostgreSQL + HikariCP JSON generated.
- `./gradlew :javers-exposed:compileKotlin :examples-javers-exposed-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 4 tests, final PostgreSQL + HikariCP JSON generated.
- `./gradlew :javers-exposed:test --tests '*snapshot and commit tables declare natural keys and hot path indexes*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, H2/PostgreSQL/MySQL matrix.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 53 tests.
- `xmllint --noout docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg`
  - PASS.
- PNG chart generated with `rsvg-convert` and visually inspected.
- `git diff --check`
  - PASS.

## Future Guidance

Do not compare audit benchmark rows unless the payload shape is clear. If a
README states performance direction, include metric direction, environment,
raw artifact link, and a bounded-scope caveat. For durable performance claims,
move from this JUnit documentation benchmark to a dedicated JMH benchmark.
If a future PR wants `author` or `commit_date` SQL pushdown indexes, evaluate
them separately with insert-cost measurements because those indexes live on the
commit table and increase write overhead. Follow-up: #188.
