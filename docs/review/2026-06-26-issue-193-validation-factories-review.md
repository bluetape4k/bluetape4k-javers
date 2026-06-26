# Issue 193 Review Notes

## Scope

Reviewed the issue-listed validation factory alignment targets:

- `javers-exposed/.../JaversExposedTables.kt`
- `javers-core/.../CdoSnapshotEvent.kt`
- `examples/javers-exposed-ddd/.../domain/Order.kt`
- `examples/javers-spring-boot4/.../domain/Order.kt`
- `examples/javers-ktor/.../domain/Order.kt`

## Findings

No P0/P1 findings in the final diff.

## Evidence

- The affected table-name and order aggregate data classes now use private
  constructors plus companion factories.
- The example order factories reject empty items, non-positive quantities, and
  non-positive unit prices in direct regression tests.
- `CdoSnapshotEventMetadata` numeric validation now uses bluetape4k validation
  helpers for `snapshotVersion` and `repositorySequence`.
- Targeted rerun passed:
  `./gradlew :javers-core:test :javers-exposed:test :examples-javers-exposed-ddd:test :examples-javers-ktor:test :examples-javers-spring-boot4:test --rerun-tasks --no-configuration-cache --no-build-cache --no-parallel --console=plain`

## Residual Risk

The Kotlin visibility contract is compile-time enforced. The regression tests
cover the public factory behavior, while attempted external `copy()` calls are
not represented because they would be compile failures.
