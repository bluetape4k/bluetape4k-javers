# Issue #132 - JaVers Exposed Benchmark Breakdown Review

## Scope

- `examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt`
- `examples/javers-exposed-ddd/build.gradle.kts`
- `gradle/libs.versions.toml`
- `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt`
- `javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryDatabaseSmokeTest.kt`
- `javers-exposed/README.md`
- `javers-exposed/README.ko.md`
- `docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json`
- `docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg`
- `docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png`
- `README.md`
- `README.ko.md`

## Subagent Review Follow-up

Initial read-only reviewer pass found P0=0, P1=3, P2=3 before final edits.

| Finding | Fix |
|---|---|
| `JaVers + Exposed` mixed source-table persistence with snapshot repository cost. | Split the benchmark into `JaVers + Exposed repository` and `JaVers + Exposed DDD path`. README now labels the DDD path as end-to-end example orchestration. |
| Envers audit query only loaded revision ids while JaVers loaded snapshots. | Envers audit query now loads audited entity revisions with `AuditReader.find(...)`. |
| README still carried the stale 105.339 ms/op conclusion. | README English/Korean now link the 2026-06-08 artifact, embed the chart, and state that the outlier is not reproduced. |

Follow-up verdict: P0=0, P1=0.

Natural-key follow-up reviewer pass found P0=0, P1=1, P2=1.

| Finding | Fix |
|---|---|
| Existing schema metadata test expected `(global_id, version)` to appear as a unique index. | Updated the test to assert snapshot `(global_id, version)` primary key and commit `commit_id` primary key instead. |
| `author` and `commit_date` SQL pushdown filters have no secondary indexes. | Kept them as a follow-up tradeoff instead of adding more indexes in this insert-cost-focused PR; commit-table indexes are tracked in #188. |

Natural-key verdict: P0=0, P1=0. Residual P2 is tracked as #188, not a correctness blocker.

## 7-Tier Final Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Benchmark JSON, chart, README | No secrets, raw user identifiers, or production network endpoints are introduced. Testcontainers credentials are local fixture defaults. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness | Benchmark lane definitions and schema keys | The four lanes separate Hibernate Envers, JaVers in-memory, Exposed snapshot repository, and the full DDD example path. Commit rows use `commit_id` PK; snapshot rows use `(global_id, version)` PK, matching JaVers snapshot identity. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module and artifact boundaries | Changes stay in Exposed schema/test docs, benchmark test/dependencies, root/module README locale pairs, and benchmark assets. No module registration, CI, Nightly, or BOM change is required. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Test generator and schema code | Kotlin changes keep helpers private where possible, use `Base58.randomString(6)` for unique pool/table names, and keep public schema KDoc aligned with the natural-key contract. Existing serializable data classes keep `serialVersionUID`. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Generated artifact and assertions | The targeted benchmark test regenerates the 2026-06-08 JSON, asserts every measured lane has positive ms/op, and schema smoke tests now verify natural primary keys directly. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Benchmark interpretation and index tradeoff | The result is explicitly bounded to PostgreSQL Testcontainers + HikariCP documentation use. Surrogate id indexes are removed from JaVers tables; additional commit `author`/`commit_date` indexes remain a separate tradeoff because they add insert cost. Follow-up: #188. | P0=0, P1=0, P2=1, P3=0 |
| 7 Docs/release/evidence | README, chart, raw JSON | README English/Korean share the same table, chart, command, artifact link, environment, metric direction, and caveat. The chart has SVG+PNG assets and English labels. | P0=0, P1=0, P2=0, P3=0 |

Final verdict: PASS with P0=0 and P1=0. P2=1 is tracked in #188 as a follow-up performance-index tradeoff.

## Validation Evidence

- `./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 1 test executed, PostgreSQL + HikariCP benchmark JSON generated.
- `./gradlew :javers-exposed:compileKotlin :examples-javers-exposed-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
- `./gradlew :javers-exposed:test --tests '*snapshot and commit tables declare natural keys and hot path indexes*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 3 DB matrix tests executed.
- `xmllint --noout docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg`
  - Result: PASS.
- `rsvg-convert docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg -o docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png`
  - Result: PASS.
- Visual inspection of `docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png`
  - Result: PASS, chart is nonblank and labels do not overlap.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 4 tests executed, final benchmark JSON generated.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 53 tests executed.
- `rg -n "UUID|!!|runBlocking|GlobalScope|Thread\\.sleep|synchronized\\s*\\(|@Synchronized|runCatching" examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt`
  - Result: PASS, no matches.
- `git diff --check`
  - Result: PASS.
