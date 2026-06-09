# Issue 104 Spring Boot Auto-Configuration

## Context

Spring Boot auto-configuration for JaVers repositories crossed code, docs,
module registration, and workflow coverage.

## Decision

Keep all infrastructure clients application-owned and make each repository
backend a directly registered auto-configuration phase with class-name guards.

## Outcome

The new module registers Exposed, Lettuce, Redisson, Spring Kafka, and vanilla
Kafka repository backends only when explicitly selected and when the required
bean/class boundary exists.

## Verification

- `./gradlew :javers-spring-boot4-autoconfigure:test ...` passed with 13 tests.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` passed.
- `./gradlew build -x test ...` passed.

## Future Guard

For new Spring Boot auto-configuration modules, update `AutoConfiguration.imports`,
README locale files, CHANGELOG, `AGENTS.md`, CI, Nightly, and Kover artifacts in
the same PR. Guard compile-only bean signatures with `@ConditionalOnClass(name = [...])`.
