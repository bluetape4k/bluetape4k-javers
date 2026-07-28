# Issue #103 Exposed SQL Pushdown 리뷰

## 범위

- Issue: #103, `feat: add SQL pushdown for common javers-exposed JQL filters`
- Branch: `feat/issue-103-exposed-sql-pushdown`
- Base: `develop` at `06934f8`
- 변경 파일:
  - `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt`
  - `javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryDatabaseSmokeTest.kt`
  - `javers-exposed/README.md`
  - `javers-exposed/README.ko.md`

## 7-Tier 검토 결과

| Tier | 영역 | P0 | P1 | 비고 |
|---|---:|---:|---:|---|
| 1 | 정확성 | 0 | 0 | SQL pushdown은 영속 컬럼으로 뒷받침되는 필터로 제한된다. 지원하지 않는 predicate는 공유 repository 경로로 fallback한다. |
| 2 | API / 호환성 | 0 | 0 | 공개 API 변경은 없다. repository override는 JaVers `JaversRepository` 시그니처를 유지한다. |
| 3 | 영속성 / 트랜잭션 | 0 | 0 | 모든 SQL read는 기존 Exposed transaction wrapper를 계속 통과한다. |
| 4 | 성능 | 0 | 0 | counting codec 테스트가 author, limit, skip+limit, class-history 경로에서 SQL로 선택된 row만 decode함을 증명한다. |
| 5 | 테스트 | 0 | 0 | H2, PostgreSQL, MySQL matrix가 pushdown과 fallback 동작을 모두 커버한다. |
| 6 | 문서 | 0 | 0 | English/Korean README의 query behavior 섹션이 지원되는 pushdown과 fallback predicate를 설명한다. |
| 7 | Workflow / 증거 | 0 | 0 | 구현 전에 issue 본문을 갱신했고, PR 생성 전에 review 증거를 기록했다. |

## 증거

- GNO preflight:
  - `gno query "bluetape4k-javers 0.3.0 issue 103 exposed schema mapping" -c bluetape4k-github --fast --no-rerank`
  - `gno query "bluetape4k-javers 0.3.0 issue 103 exposed schema mapping" -c bluetape4k-docs --fast --no-rerank`
- Issue 갱신:
  - #103 본문을 현재 `develop=06934f8` 및 #106 이후 선행 조건에 맞게 갱신했다.
- Compile:
  - `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain` PASS.
- Targeted DB matrix:
  - `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - 36 tests executed: H2, PostgreSQL, MySQL.
- Module regression:
  - `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - 46 tests executed.
- Code Review Graph:
  - 이 worktree의 graph stats가 `Files: 0`, `Last updated: never`로 보고되어 graph impact data는 증거로 사용하지 않았다.
- IntelliJ diagnostics:
  - 이 세션에서 IntelliJ MCP diagnostics tool을 사용할 수 없어 Gradle compile과 targeted tests를 fallback evidence로 사용했다.

## Gate 판정

- P0=0
- P1=0
- Gate: PASS

## 잔여 위험

- Commit property, changed property, author-like, `Instant`, version-range, `toCommitId`, aggregate, `snapshotQueryLimit` query는 의도적으로 기존 in-memory filtering 경로에 남겨 둔다.
