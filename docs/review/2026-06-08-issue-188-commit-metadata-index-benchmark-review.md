# Issue #188 7-Tier review

## 판정

Gate: PR creation 기준 PASS.

| Severity | Count |
|---|---:|
| P0 | 0 |
| P1 | 0 |
| P2 | 0 |
| P3 | 0 |

## T1 정확성

PASS. benchmark는 trial별 JaVers Exposed table을 만들고 optional candidate index를
그 temporary table에만 적용한 뒤 teardown에서 drop한다. Production
`CommitTableMapping`은 변경하지 않았다.

## T2 API 및 Schema Compatibility

PASS. public JaVers Exposed API 또는 default table schema 변경은 없다. benchmark
module은 `benchmark/` 아래 등록되며 root example/benchmark predicate에 의해 normal
publication에서 제외된다.

## T3 Performance 및 Benchmark Validity

bounded evidence 기준 PASS. benchmark는 JUnit timing loop가 아니라
`kotlinx-benchmark`/JMH를 사용한다. Testcontainers와 HikariCP를 통해 PostgreSQL
18-alpine에 대해 실행하며, corpus를 preload하고 `ANALYZE`를 실행한 뒤 `baseline`,
`author`, `commit_date`, `both` variant를 비교한다.

smoke result는 permanent DDL policy를 증명할 만큼 넓지 않다. 따라서 구현은 결과를
올바르게 문서화하고 production default를 변경하지 않는다.

## T4 Tests 및 Verification

PASS. Local verification:

- `./gradlew :benchmark-javers-exposed-benchmark:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `xmllint --noout docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg`
- `rsvg-convert docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg -o docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png`

CodeGraph note: worktree graph가 zero nodes를 반환했으므로 이 diff에 유용한
structural review evidence를 제공하지 못했다.

## T5 Docs 및 UX

PASS. Root 및 example README는 benchmark command, unit(`ops/s`), raw JSON
artifact, chart, production-schema decision을 문서화한다. English/Korean README
variant를 함께 갱신했다.

## T6 Maintainability

PASS. benchmark는 bluetape4k ecosystem helper를 재사용한다.

- `hikariDataSourceOf` and `withStatement` from `bluetape4k-jdbc`
- `execCreateMissingTablesAndColumns` from `bluetape4k-exposed-jdbc`
- `TestDB.POSTGRESQL` from `bluetape4k-exposed-jdbc-tests`

## T7 Release 및 Workflow

PASS. 변경은 issue-backed 및 milestone-scoped이며, published artifact 또는
production DDL migration을 도입하지 않는다.

## 잔여 위험

smoke benchmark는 의도적으로 짧고 JMH score는 run마다 달라질 수 있다. 결과는 final
capacity model이 아니라 current bounded evidence로 다뤄야 한다. production schema를
변경하기 전 더 큰 benchmark는 repeated iteration과 planner artifact를 추가해야 한다.
