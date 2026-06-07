# Issue 106 - Exposed Schema Options

## Context

Issue #106 added repository-local Exposed table mapping options for `javers-exposed`.

## Decision

Keep the public option surface table-name centric, but derive custom index names from custom table names when building `ExposedJaversSchema`.

## Outcome

An initial H2 test failed because a custom commit table hit an Exposed/H2 unique constraint name collision. Deriving index names from the custom table name fixed the failure and kept default singleton tables source-compatible.

## Verification

- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --console=plain` - PASS, 31 tests

## Future Guard

When adding Exposed table-name customization, check index and constraint names in the same change. Table-name configurability without index-name isolation can still collide in one database.
