# Issue 101 Exposed Indexes

## Context

`javers-exposed` loads snapshot history by `global_id` ordered by `version` and
restores repository head state by selecting the highest commit sequence.

## Decision

Keep the existing unique `(global_id, version)` index as the snapshot history
hot-path index, expose its name as a schema constant for tests, and add an
explicit `sequence` index on `javers_commit` for head restoration.

## Outcome

Repository-specific scenarios now run through the shared Exposed JDBC dialect
matrix instead of relying only on H2-only coverage.

## Verification

`./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
passed with H2, PostgreSQL, and MySQL_V8 matrix coverage for the repository
scenarios.
