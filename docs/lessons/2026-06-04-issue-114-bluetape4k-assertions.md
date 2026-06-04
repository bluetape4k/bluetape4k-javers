# Issue 114 bluetape4k Assertions Cleanup

## Context

Issue #114 found one remaining `org.junit.jupiter.api.assertThrows` import in
`javers-core` tests.

## Decision

Replace the JUnit exception assertion with
`io.bluetape4k.assertions.assertFailsWith` while preserving the same
`JaversException` contract.

## Outcome

`AbstractJaversShadowTest` now uses bluetape4k assertions consistently for the
exception assertion.

## Verification Evidence

- Assertion API scan over `javers-core/src/test`
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`

## Future Guidance

When touching tests, scan for JUnit, AssertJ, Kluent, and `kotlin.test`
assertions before closing the work item.
