# Issue #117 - JDK Codec Gate Review

## Scope

- `javers-core` codec registry and codec tests.
- `javers-core` README locale set.
- Build dependency exposure for the bluetape4k API marker annotation.

## 7-Tier Result

| Tier | Area | P0 | P1 | P2 | P3 | Notes |
|---|---:|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | JDK serializer aliases are retained only as obsolete compatibility bridges and now fail new Kotlin call sites through `@BluetapeObsoleteApi` plus `DeprecationLevel.ERROR`. |
| 2 | Correctness | 0 | 0 | 0 | 0 | Kryo/Fory binary codec coverage remains; JDK round-trip coverage was intentionally removed from the normal codec matrix. |
| 3 | Runtime/lifecycle | 0 | 0 | 0 | 0 | No coroutine, blocking, connection, or resource lifecycle behavior changed. |
| 4 | Kotlin/API quality | 0 | 0 | 0 | 0 | Public compatibility symbols remain available, but new use requires explicit obsolete opt-in and receives replacement guidance. |
| 5 | Tests | 0 | 0 | 0 | 0 | `:javers-core:test` passed with 191 tests. |
| 6 | Docs/build | 0 | 0 | 0 | 0 | README and README.ko document codec safety and migration choices; `javers-core` exposes `bluetape4k-annotations` as an API dependency. |
| 7 | Process/evidence | 0 | 0 | 0 | 0 | CodeGraph stats were available but stale; direct source inspection, GNO precedent, and Gradle validation were used. |

## Validation

- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed.
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed, 191 tests.
- `./gradlew compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed. Existing unrelated `SchemaUtils.createMissingTablesAndColumns` warning remains tracked by #194.
- `git diff --check` - passed.
- `rg -n "JaversCodecs\\.(Jdk|DeflateJdk|GZipJdk|LZ4Jdk|SnappyJdk|ZstdJdk)|BinarySerializers\\.Jdk|JDK-serialization|JDK serialization|Java deserialization" . -g '*.kt' -g '*.md' -g '*.kts'` - JDK references are limited to obsolete bridge declarations and migration docs.

## Verdict

P0=0, P1=0. The issue #117 security gate is addressed for the 0.3.0 readiness path.
