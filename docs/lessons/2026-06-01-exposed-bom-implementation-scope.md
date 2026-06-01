# Exposed BOM implementation scope

## Context

The `bluetape4k-dependencies 1.2.0` train promotes
`bluetape4k-exposed-bom` to the `1.10.0` line. `javers-exposed` consumes
bluetape4k Exposed integration artifacts, but the BOM platform itself should
not be part of the public API surface.

## Decision

Import `bluetape4k-exposed-bom` with `implementation(platform(...))` in
`javers-exposed`.

## Outcome

The module remains aligned with the train catalog while avoiding an API-scoped
BOM platform export.

## Verification

- Maven Central returned HTTP 200 for `bluetape4k-exposed-bom:1.10.0`.
- `./gradlew :javers-exposed:build --no-daemon --console=plain` passed.

## Future Guidance

Keep concrete public Exposed artifacts on `api` only when their types are part
of the public contract; keep the bluetape4k Exposed BOM platform internal.
