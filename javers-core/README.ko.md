# Module bluetape4k-javers-core

[English](./README.md) | 한국어

[JaVers](https://javers.org)를 Kotlin 서비스에서 편하게 쓰기 위한 extension
layer입니다. 다른 `bluetape4k-javers` 모듈이 공유하는 codec, repository, JQL,
metamodel, convenience API를 제공합니다.

## 기능

- JaVers comparison, snapshot, shadow, JQL workflow용 Kotlin extension.
- String, binary, compressed snapshot payload용 `JaversCodec` 구현.
- cache, Redis, Kafka, Exposed snapshot repository가 공유하는
  `AbstractCdoSnapshotRepository`.
- Caffeine, Cache2k, JCache 기반 cache-backed repository 구현.
- cluster-friendly commit metadata를 위한 Snowflake commit id generator.

## 사용 예

```kotlin
val javers = JaversBuilder.javers()
    .build()

val diff = javers.compare(oldOrder, newOrder)
val snapshot = javers.latestSnapshotOrNull<Order>(orderId)
```

SQL, Redis, Kafka persistence adapter 없이 JaVers helper API만 필요할 때
`javers-core`를 직접 사용하세요.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-core")
}
```

## 빌드

```bash
./gradlew :javers-core:test
```
