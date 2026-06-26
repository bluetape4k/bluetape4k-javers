# 2026-06-01 Open 0.3.0 Development

## Context

The `0.2.1` patch lane was prepared for release-train dependency alignment,
but the stable tag and GitHub release were not published at that point.

## Decision

Move the committed `baseVersion` to `0.3.0` while keeping `snapshotVersion=`
empty so release workflows can inject snapshot qualifiers explicitly.
Align the direct `bluetape4k-bom` catalog reference to
`1.11.0-SNAPSHOT`.

## Outcome

The repository is ready for the next minor development line, but the `0.2.1`
patch release must still be tagged, published, and verified before a downstream
stable dependencies train can consume it.

## Verification

- `gradle.properties` uses `baseVersion=0.3.0`.
- `snapshotVersion=` remains empty.
- `./gradlew help --no-daemon --console=plain` resolves the updated catalog.
- Follow-up audit on 2026-06-26 found GitHub Release `0.2.0` only; `0.2.1`
  was not yet tagged or published.
