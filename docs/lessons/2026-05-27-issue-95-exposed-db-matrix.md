# Issue 95 Exposed DB Matrix

## Context

`javers-exposed` had a database smoke test with local PostgreSQL/MySQL
Testcontainers wiring. The workspace already provides the shared
`bluetape4k-exposed-jdbc-tests` matrix helpers.

## Decision

Reuse `bluetape4k-exposed-jdbc` and `bluetape4k-exposed-jdbc-tests` through the
central `bt4k` catalog. Test through `AbstractExposedTest`,
`@MethodSource(ENABLE_DIALECTS_METHOD)`, and `withTables` so the default matrix
stays aligned with Exposed: H2, PostgreSQL, and MySQL_V8. Use JUnit
`Assumptions` only when a scenario is genuinely unsupported by a dialect.

## Outcome

The smoke test now runs on the shared Exposed matrix. `saveSnapshot()` no longer
depends on Exposed `insertIgnore`, which failed on standard H2; commit metadata
is inserted with a dialect-neutral existence check.

## Verification

- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passed 3 tests: H2, PostgreSQL, MYSQL_V8.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passed 10 tests.

## Future Guidance

When adding `javers-exposed` database coverage, prefer the Exposed shared test
helpers over ad hoc containers. Keep mandatory baseline scenarios portable
across H2/PostgreSQL/MySQL_V8; reserve `Assumptions` for explicitly unsupported
database features.
