# Issue 106 Local Review - Exposed Schema Options

## 범위

- Issue: #106, `feat: make Exposed schema mapping configurable`
- Branch: `feat/issue-106-exposed-schema-options`
- Module: `javers-exposed`
- 변경 표면:
  - `ExposedCdoSnapshotRepositoryOptions`
  - `ExposedJaversTableNames`
  - `ExposedJaversSchema`
  - configurable `CommitTableMapping` / `CdoSnapshotTableMapping`

## 리뷰 결과

- P0: 0
- P1: 0
- Gate: PASS

## 7-Tier 점검

| Tier | 결과 | 증거 |
|---|---:|---|
| API 호환성 | PASS | 기존 constructor call 형태는 source-compatible하게 유지된다. `database`와 `codec` 기본값은 변경하지 않고 `options`만 뒤에 추가했다. |
| 영속성 의미 | PASS | Repository는 여전히 하나의 Exposed transaction 경로에서 commit metadata와 snapshot row를 쓰며, 이제 instance-local table mapping을 사용한다. |
| Schema ownership | PASS | H2 테스트가 `createSchemaOnEnsure=false`를 커버한다. `ensureSchema()`는 no-op이고, 외부 `SchemaUtils.create(*schema.tables)`가 write를 가능하게 한다. |
| Dialect coverage | PASS | H2, PostgreSQL, MySQL v8 matrix가 custom table name을 커버한다. |
| Index 안전성 | PASS | 같은 database 안의 index/constraint 충돌을 피하도록 custom schema가 custom table name에서 index name을 파생한다. |
| 문서 | PASS | `javers-exposed/README.md`와 `README.ko.md`가 custom table name과 external migration ownership을 문서화한다. |
| Tooling fallback | PASS | CodeGraph는 확인했지만 stale 상태였다(`last_updated=2026-05-17`). 직접 source/test inspection과 Gradle verification을 fallback으로 사용했다. 이 세션에서 IntelliJ diagnostics MCP는 사용할 수 없었다. |

## 검증 증거

- `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain` - PASS
- `./gradlew :javers-exposed:test --tests 'io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryH2Test' --no-configuration-cache --no-build-cache --console=plain` - PASS, 8 tests
- `./gradlew :javers-exposed:test --tests 'io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryDatabaseSmokeTest' --no-configuration-cache --no-build-cache --console=plain` - PASS, 21 tests
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --console=plain` - PASS, 31 tests

## Index 평가

- `CommitTable.commit_id` unique index는 적절하다. commit existence check, sequence lookup, sequence update에서 사용된다.
- `CommitTable.sequence` index는 적절하다. 가장 높은 repository sequence로 정렬하는 `loadHeadId()`를 지원한다.
- `CdoSnapshotTable` unique `(global_id, version)` index는 적절하다. `global_id` 기준 snapshot history lookup, `version` 기준 newest-first ordering, size check, identifier lookup을 지원한다.
- 추가 snapshot single-column index가 이 PR에서 index를 더 추가해야 할 이유는 아니다. 현재 `QueryParams` filtering은 snapshot load 이후 inherited in-memory filtering을 계속 통과하므로 commit id, type, changed-property, author, date SQL index는 전용 SQL pushdown 변경에서 다루는 것이 맞다.
- 이 PR의 index 관련 변경은 custom table name에서 custom index name을 파생하는 데 한정된다. disabled-schema test path에서 H2가 실제 same-database constraint-name collision을 드러냈으므로 이 변경은 적절하다.

## 잔여 위험

- Custom schema/catalog placement는 여전히 Exposed 또는 external migration tooling에 위임된다. 이 변경은 table name과 schema creation ownership을 제어하며, database-specific catalog DDL을 제어하지 않는다.
