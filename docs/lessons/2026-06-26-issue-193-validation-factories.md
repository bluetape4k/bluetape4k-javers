# Issue 193 Validation Factory Alignment

## Context

Issue #193 identified remaining data classes that still validated constructor
arguments in `init` blocks while exposing public `copy()` paths.

## Decision

Use `@ConsistentCopyVisibility` with private constructors and companion
factories for the affected value/aggregate types:

- `ExposedJaversTableNames`
- the three example `Order` aggregates

Keep validation in the companion factory and add direct factory regression tests
for example aggregates so invalid item lists cannot bypass validation through
normal construction paths.

## Verification

- `rg "init \\{|require\\(items" javers-core javers-exposed examples/javers-exposed-ddd examples/javers-spring-boot4 examples/javers-ktor --glob '*.kt'`
- `./gradlew :javers-core:test :javers-exposed:test :examples-javers-exposed-ddd:test :examples-javers-ktor:test :examples-javers-spring-boot4:test --rerun-tasks --no-configuration-cache --no-build-cache --no-parallel --console=plain`

The Gradle run completed successfully with:

- `:javers-core:test`: 191 tests
- `:javers-exposed:test`: 53 tests
- `:examples-javers-exposed-ddd:test`: 6 tests
- `:examples-javers-spring-boot4:test`: 6 tests
- `:examples-javers-ktor:test`: 7 tests

## Follow-up

The generic Gradle 10 deprecation summary remains outside this issue's source
changes and should be handled in the broader Gradle release-prep lane.
