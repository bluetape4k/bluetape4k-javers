# Issue #103 Exposed SQL Pushdown Review

## Scope

- Issue: #103, `feat: add SQL pushdown for common javers-exposed JQL filters`
- Branch: `feat/issue-103-exposed-sql-pushdown`
- Base: `develop` at `06934f8`
- Changed files:
  - `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt`
  - `javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryDatabaseSmokeTest.kt`
  - `javers-exposed/README.md`
  - `javers-exposed/README.ko.md`

## 7-Tier Findings

| Tier | Area | P0 | P1 | Notes |
|---|---:|---:|---:|---|
| 1 | Correctness | 0 | 0 | SQL pushdown is limited to filters backed by persisted columns; unsupported predicates fall back to the shared repository path. |
| 2 | API / Compatibility | 0 | 0 | No public API change; repository overrides preserve JaVers `JaversRepository` signatures. |
| 3 | Persistence / Transactions | 0 | 0 | All SQL reads still run through the existing Exposed transaction wrapper. |
| 4 | Performance | 0 | 0 | Counting codec tests prove author, limit, skip+limit, and class-history paths decode only SQL-selected rows. |
| 5 | Tests | 0 | 0 | H2, PostgreSQL, and MySQL matrix covers pushdown and fallback behavior. |
| 6 | Documentation | 0 | 0 | English/Korean README query behavior sections now describe supported pushdown and fallback predicates. |
| 7 | Workflow / Evidence | 0 | 0 | Issue body was refreshed before implementation; review evidence is tracked before PR creation. |

## Evidence

- GNO preflight:
  - `gno query "bluetape4k-javers 0.3.0 issue 103 exposed schema mapping" -c bluetape4k-github --fast --no-rerank`
  - `gno query "bluetape4k-javers 0.3.0 issue 103 exposed schema mapping" -c bluetape4k-docs --fast --no-rerank`
- Issue refresh:
  - #103 body updated to current `develop=06934f8` and post-#106 prerequisites.
- Compile:
  - `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain` PASS.
- Targeted DB matrix:
  - `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - 36 tests executed: H2, PostgreSQL, MySQL.
- Module regression:
  - `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - 46 tests executed.
- Code Review Graph:
  - Graph stats for this worktree reported `Files: 0`, `Last updated: never`; graph impact data was not used as proof.
- IntelliJ diagnostics:
  - IntelliJ MCP diagnostics tool was not available in this session; Gradle compile and targeted tests are the fallback evidence.

## Gate Verdict

- P0=0
- P1=0
- Gate: PASS

## Residual Risk

- Commit property, changed property, author-like, `Instant`, version-range, `toCommitId`, aggregate, and `snapshotQueryLimit` queries intentionally stay on the existing in-memory filtering path.
