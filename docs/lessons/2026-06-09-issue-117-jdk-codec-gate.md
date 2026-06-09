# Issue #117 - JDK Codec Gate

## Context

`JaversCodecs.Jdk` and compressed JDK-backed aliases exposed Java native
serialization as ordinary codec choices. The upstream bluetape4k serializer is
deprecated because Java deserialization is unsafe for untrusted bytes.

## Decision

Keep the public aliases only as compatibility bridges, but mark each one with
`@BluetapeObsoleteApi` and `@Deprecated(level = DeprecationLevel.ERROR)`.
Normal codec tests now cover Kryo/Fory binary codecs, not JDK deserialization.

## Outcome

- New Kotlin call sites cannot use JDK-backed JaVers codecs without deliberate
  obsolete opt-in.
- README and README.ko point users to string, Kryo, or Fory codecs.
- `javers-core` exposes `bluetape4k-annotations` as an API dependency because
  the marker is part of the public API surface.

## Verification

- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`

## Future Guidance

Do not add new tests or examples that deserialize JDK-backed payloads. If a
compatibility test is needed, isolate it behind explicit obsolete opt-in and
state why Kryo/Fory or JSON cannot prove the same behavior.
