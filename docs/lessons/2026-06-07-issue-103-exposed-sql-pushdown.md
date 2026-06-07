# Issue #103 Exposed SQL Pushdown

## Context

`javers-exposed` previously used the shared `AbstractCdoSnapshotRepository`
query path for broad JaVers snapshot queries. That path loads all repository
keys and decodes snapshots before applying common query filters.

## Decision

Push down only predicates backed by durable Exposed columns:

- global id history for non-aggregate queries
- managed type history for non-aggregate class queries
- commit ids, exact version, exact author, `LocalDateTime` commit date range,
  snapshot type, `skip`, and `limit` for `getSnapshots(QueryParams)`

Keep predicates that need JaVers in-memory semantics on the existing fallback
path.

## Outcome

The repository now reduces decoded snapshot rows for common JQL paths without
changing public API contracts or the stored snapshot payload format.

## Verification

- `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain`
- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- DB matrix executed 36 tests across H2, PostgreSQL, and MySQL.
- Full `:javers-exposed:test` executed 46 tests.

## Future Guard

When adding more SQL pushdown, first prove the predicate is backed by an
explicit persisted column or a dialect-safe expression. If not, keep the shared
repository fallback and document the unsupported predicate in README.
