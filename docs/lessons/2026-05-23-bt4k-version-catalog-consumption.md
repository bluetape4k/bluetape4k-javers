# bt4k Version Catalog Consumption

## Context

`bluetape4k-javers` carried local pins for common dependencies that are already
managed by the shared ecosystem catalog.

## Decision

Import the `bluetape4k-dependencies` version catalog as `bt4k` and source shared
leaf dependency versions from `bt4kVersion(alias)` inside dependency management.

## Outcome

Selected dependency aliases are versionless in the local catalog, with versions
resolved from the shared catalog during Gradle dependency management.

## Verification

- `git diff --check`
- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew compileKotlin --no-daemon --no-configuration-cache`

## Future Guidance

Use `bt4k` for common serialization, cache, logging, and compression dependency
versions before adding local version pins.
