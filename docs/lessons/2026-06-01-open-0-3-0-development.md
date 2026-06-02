# 2026-06-01 Open 0.3.0 Development

## Context

`bluetape4k-javers` `0.2.1` was published for release-train dependency
alignment.

## Decision

Move the committed `baseVersion` to `0.3.0` while keeping `snapshotVersion=`
empty so release workflows can inject snapshot qualifiers explicitly.
Align the direct `bluetape4k-bom` catalog reference to
`1.11.0-SNAPSHOT`.

## Outcome

The repository is ready for the next minor development line.

## Verification

- `gradle.properties` uses `baseVersion=0.3.0`.
- `snapshotVersion=` remains empty.
- `./gradlew help --no-daemon --console=plain` resolves the updated catalog.
