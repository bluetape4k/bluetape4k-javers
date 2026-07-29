# Issue #117 - JDK Codec Gate 검토

## 범위

- `javers-core` codec registry and codec tests.
- `javers-core` README locale set.
- bluetape4k API marker annotation을 위한 build dependency exposure.

## 7-Tier 결과

| Tier | 영역 | P0 | P1 | P2 | P3 | 비고 |
|---|---:|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | JDK serializer alias는 obsolete compatibility bridge로만 유지되며, 이제 새 Kotlin call site는 `@BluetapeObsoleteApi`와 `DeprecationLevel.ERROR`를 통해 실패한다. |
| 2 | Correctness | 0 | 0 | 0 | 0 | Kryo/Fory binary codec coverage는 유지된다. JDK round-trip coverage는 normal codec matrix에서 의도적으로 제거했다. |
| 3 | Runtime/lifecycle | 0 | 0 | 0 | 0 | coroutine, blocking, connection, resource lifecycle behavior 변경은 없다. |
| 4 | Kotlin/API quality | 0 | 0 | 0 | 0 | Public compatibility symbol은 계속 사용할 수 있지만, 새 사용은 explicit obsolete opt-in이 필요하고 replacement guidance를 받는다. |
| 5 | Tests | 0 | 0 | 0 | 0 | `:javers-core:test`가 191 tests로 통과했다. |
| 6 | Docs/build | 0 | 0 | 0 | 0 | README와 README.ko는 codec safety와 migration choice를 문서화한다. `javers-core`는 `bluetape4k-annotations`를 API dependency로 노출한다. |
| 7 | Process/evidence | 0 | 0 | 0 | 0 | CodeGraph stats는 사용할 수 있었지만 stale했다. direct source inspection, GNO precedent, Gradle validation을 사용했다. |

## 검증

- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed.
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed, 191 tests.
- `./gradlew compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` - passed. 기존 unrelated `SchemaUtils.createMissingTablesAndColumns` warning은 #194에서 계속 추적한다.
- `git diff --check` - passed.
- `rg -n "JaversCodecs\\.(Jdk|DeflateJdk|GZipJdk|LZ4Jdk|SnappyJdk|ZstdJdk)|BinarySerializers\\.Jdk|JDK-serialization|JDK serialization|Java deserialization" . -g '*.kt' -g '*.md' -g '*.kts'` - JDK reference는 obsolete bridge declaration과 migration docs로 제한된다.

## 판정

P0=0, P1=0. Issue #117 security gate는 0.3.0 readiness path에서 처리됐다.
