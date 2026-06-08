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
- primary durable storage와 ordered secondary fanout을 조합하는
  `CompositeCdoSnapshotRepository`.
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

## Composite Repository

![Composite CDO snapshot repository](./docs/images/readme-diagrams/javers-core-composite-repository-01.png)

하나의 read/query source of truth와 여러 write-side fanout target이 필요할 때
`CompositeCdoSnapshotRepository`를 사용합니다.

```kotlin
val repository = CompositeCdoSnapshotRepository(
    primary = exposedRepository,
    secondaryRepositories = listOf(
        redisProjectionRepository,
        kafkaRepository,
    ),
    options = CompositeCdoSnapshotRepositoryOptions(
        writeFailurePolicy = CompositeCdoSnapshotFailurePolicy.FAIL_FAST,
    ),
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

권장 조합:

| Shape | Primary | Secondary repositories | Notes |
|---|---|---|---|
| Durable history + events | Exposed | Kafka | SQL은 JaVers query source, Kafka는 audit event stream입니다. |
| Durable history + projection + events | Exposed | Redis, Kafka | Redis는 명시적인 projection/cache store이며 hidden write-behind가 아닙니다. |
| Redis-first audit store + events | Redis | Kafka | Redis를 direct JaVers snapshot repository로 수용할 때만 사용합니다. |

write는 primary-first입니다. `persist(commit)`은 primary repository의 native
`persist()`를 먼저 호출해 primary의 head/sequence semantics를 보존한 뒤,
secondary repository들에 순서대로 fanout합니다. 이 동작은 distributed
transaction이 아닙니다. secondary가 실패하면 primary에는 이미 commit이 보일
수 있습니다. `FAIL_FAST`는 첫 secondary 실패에서 중단하고, `BEST_EFFORT`는
모든 secondary를 시도한 뒤 aggregate exception을 던집니다.

Kafka repository는 계속 write-only입니다. Kafka record를 read-capable
repository로 replay해야 하면 `javers-persistence-kafka`의
`KafkaCdoSnapshotProjector`를 사용하세요.

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
