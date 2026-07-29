# Issue #132 - JaVers Exposed Benchmark Breakdown Review

## 범위

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

## Subagent Review 후속 조치

Initial read-only reviewer pass는 final edit 전 P0=0, P1=3, P2=3을 발견했다.

| 결과 | 수정 |
|---|---|
| `JaVers + Exposed`가 source-table persistence와 snapshot repository cost를 섞고 있었다. | benchmark를 `JaVers + Exposed repository`와 `JaVers + Exposed DDD path`로 분리했다. README는 이제 DDD path를 end-to-end example orchestration으로 표시한다. |
| Envers audit query는 JaVers가 snapshot을 load하는 동안 revision id만 load했다. | Envers audit query가 이제 `AuditReader.find(...)`로 audited entity revision을 load한다. |
| README에는 stale 105.339 ms/op conclusion이 남아 있었다. | README English/Korean은 이제 2026-06-08 artifact를 link하고 chart를 embed하며, outlier가 재현되지 않는다고 명시한다. |

Follow-up 판정: P0=0, P1=0.

Natural-key follow-up reviewer pass는 P0=0, P1=1, P2=1을 발견했다.

| 결과 | 수정 |
|---|---|
| 기존 schema metadata test는 `(global_id, version)`이 unique index로 나타나길 기대했다. | 대신 snapshot `(global_id, version)` primary key와 commit `commit_id` primary key를 assert하도록 test를 갱신했다. |
| `author`와 `commit_date` SQL pushdown filter에는 secondary index가 없다. | insert-cost-focused PR에서 index를 더 추가하지 않고 follow-up tradeoff로 유지했다. commit-table index는 #188에서 추적한다. |

Natural-key 판정: P0=0, P1=0. 잔여 P2는 correctness blocker가 아니라 #188에서 추적한다.

## 7-Tier Final Review

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Benchmark JSON, chart, README | secret, raw user identifier, production network endpoint를 도입하지 않았다. Testcontainers credential은 local fixture default다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness | Benchmark lane definition과 schema key | 네 lane은 Hibernate Envers, JaVers in-memory, Exposed snapshot repository, full DDD example path를 분리한다. Commit row는 `commit_id` PK를 사용하고 snapshot row는 JaVers snapshot identity와 맞는 `(global_id, version)` PK를 사용한다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module 및 artifact boundary | 변경은 Exposed schema/test docs, benchmark test/dependency, root/module README locale pair, benchmark asset 안에 머문다. module registration, CI, Nightly, BOM 변경은 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Test generator 및 schema code | Kotlin 변경은 가능한 곳에서 helper를 private으로 유지하고, unique pool/table name에는 `Base58.randomString(6)`을 사용하며, public schema KDoc을 natural-key contract와 맞춘다. 기존 serializable data class는 `serialVersionUID`를 유지한다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Generated artifact 및 assertion | targeted benchmark test는 2026-06-08 JSON을 재생성하고, 모든 measured lane의 positive ms/op를 assert하며, schema smoke tests는 natural primary key를 직접 검증한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Benchmark interpretation 및 index tradeoff | 결과는 PostgreSQL Testcontainers + HikariCP documentation use로 명시적으로 제한된다. JaVers table에서 surrogate id index를 제거했다. 추가 commit `author`/`commit_date` index는 insert cost를 추가하므로 별도 tradeoff로 남긴다. Follow-up: #188. | P0=0, P1=0, P2=1, P3=0 |
| 7 Docs/release/evidence | README, chart, raw JSON | README English/Korean은 같은 table, chart, command, artifact link, environment, metric direction, caveat를 공유한다. chart는 SVG+PNG asset과 English label을 가진다. | P0=0, P1=0, P2=0, P3=0 |

Final 판정: P0=0, P1=0으로 PASS. P2=1은 follow-up performance-index tradeoff로 #188에서 추적한다.

## 검증 증거

- `./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 1 test executed, PostgreSQL + HikariCP benchmark JSON generated.
- `./gradlew :javers-exposed:compileKotlin :examples-javers-exposed-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
- `./gradlew :javers-exposed:test --tests '*snapshot and commit tables declare natural keys and hot path indexes*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 3 DB matrix tests executed.
- `xmllint --noout docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg`
  - 결과: PASS.
- `rsvg-convert docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg -o docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png`
  - 결과: PASS.
- Visual inspection of `docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png`
  - 결과: PASS, chart is nonblank and labels do not overlap.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 4 tests executed, final benchmark JSON generated.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 53 tests executed.
- `rg -n "UUID|!!|runBlocking|GlobalScope|Thread\\.sleep|synchronized\\s*\\(|@Synchronized|runCatching" examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt`
  - 결과: PASS, no matches.
- `git diff --check`
  - 결과: PASS.
