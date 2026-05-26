# Issue 3 JaVers Exposed Repository

## Context

Issue #3 needed a new Exposed-backed JaVers repository without coupling it to
the separate #77 README diagram PR. The branch was rebuilt from `develop` after
the stacked branch accidentally included the #77 commit.

## Decision

Add `javers-exposed` as an independent module with `ExposedCdoSnapshotRepository`.
Persist each full encoded `CdoSnapshot` in `javers_snapshot`, keep commit
metadata in `javers_commit`, and store the repository sequence separately so
`loadHeadId()` can restore the latest `CommitId` after rebuilding the repository.
Leave SQL query pushdown out of the first version and rely on inherited JaVers
filtering behavior.

## Outcome

The module compiles and passes H2, PostgreSQL, and MySQL repository tests. CI and
Nightly workflows now include the new module, and README/BOM docs list the new
artifact.

## Verification

- `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-exposed:cleanTest :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## Future Work

If query volume grows, add SQL pushdown behind the same repository API. Keep the
full snapshot payload as the source of truth so JaVers JSON compatibility remains
centralized.
