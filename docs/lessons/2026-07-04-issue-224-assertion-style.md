# Issue 224 assertion-style cleanup

## Context

Issue #224 tracks a repository-wide cleanup for tests that still compared
collection sizes through scalar equality.

## Decision

Replace `collection.size shouldBeEqualTo n` with
`collection shouldHaveSize n`, and replace boolean equality with the
boolean-specific bluetape4k assertion matcher.

## Outcome

JaVers test code now follows the same bluetape4k assertion style used by newer
tests: direct collection matchers, infix equality where equality remains
appropriate, and boolean-specific matchers for boolean predicates.

## Verification Evidence

- Forbidden assertion-style scan returned no active Kotlin hits.
- `git diff --check`
- `./gradlew compileTestKotlin --no-configuration-cache`
- `./gradlew test --no-configuration-cache`
- CodeGraph affected-flow check returned 0 production flows.

## Future Guidance

When touching tests, prefer matcher intent over scalar projections:
`collection shouldHaveSize n`, `value.shouldBeTrue()`,
`value.shouldBeFalse()`, and infix `actual shouldBeEqualTo expected`.
