# Module bluetape4k-javers-core

[English](./README.md) | 한국어

`javers-core`는 Kotlin 서비스가 공유해서 쓰는 JaVers 통합 layer입니다. typed
JaVers helper API, snapshot codec, cache-backed repository, 그리고 Exposed,
Redis, Kafka 모듈이 구현하는 repository contract가 필요할 때 사용합니다.

이 모듈은 애플리케이션의 durable topology를 대신 결정하지 않습니다. 공통 계약과
local implementation을 제공할 뿐입니다. runtime audit 계약에 맞춰 SQL, Redis,
Kafka가 필요할 때만 `javers-exposed`, `javers-persistence-redis`,
`javers-persistence-kafka`를 추가하세요.

## Architecture

![javers-core architecture](../docs/images/readme-diagrams/javers-core-architecture-01.png)

## 핵심 책임

- **Kotlin JaVers extension** — typed entity mapping, collection diff, latest
  snapshot lookup, shadow reconstruction, sequence 기반 shadow query.
- **Snapshot codec family** — JSON string, compressed JSON string, trusted
  Kryo/Fory binary payload, `JsonObject` map conversion.
- **Repository base contract** — `CdoSnapshotRepository`와
  `AbstractCdoSnapshotRepository`가 snapshot encoding, head tracking, sequence
  assignment, JQL-style filtering을 공통으로 처리합니다.
- **Local cache repository** — Caffeine, Cache2k, JCache 기반 local snapshot
  repository. distributed durable store가 아닙니다.
- **Composite repository fanout** — 하나의 read/query source of truth와 순서가
  있는 secondary write target 조합.
- **Dispatcher / metadata helper** — saved/deleted object dispatch,
  Snowflake-backed commit id, snapshot event envelope.

## Class Diagram

![javers-core class diagram](../docs/images/readme-diagrams/javers-core-class-diagram-01.png)

## 빠른 시작

```kotlin
val javers = JaversBuilder.javers()
    .build()

val diff = javers.compare(oldOrder, newOrder)
val snapshot = javers.latestSnapshotOrNull<Order>(orderId)
val query = queryByInstanceId<Order>(orderId)
val shadows = javers.findShadowsAndSequence<Order>(query).toList()
```

JaVers helper API나 local cache-backed snapshot storage만 필요하면
`javers-core`를 직접 사용하세요. snapshot history가 process restart 이후에도
남아야 하거나 SQL로 query되어야 하면 Exposed adapter를 primary repository로
사용하는 편이 맞습니다.

## Codec 선택

durable JSON storage에는 `JaversCodecs.String`을 우선 사용하세요. Kryo/Fory
codec은 storage boundary가 통제된 trusted binary payload에만 사용합니다.

JDK serialization alias인 `JaversCodecs.Jdk`, `DeflateJdk`, `GZipJdk`,
`LZ4Jdk`, `SnappyJdk`, `ZstdJdk`는 obsolete compatibility bridge입니다. Java
deserialization은 untrusted bytes에 안전하지 않기 때문에 error-level
deprecated 상태입니다.

## Composite Repository

![Composite CDO snapshot repository](../docs/images/readme-diagrams/javers-core-composite-repository-01.png)

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
secondary repository들에 순서대로 fanout합니다.

이 동작은 distributed transaction이 아닙니다. secondary가 실패하면 primary에는
이미 commit이 보일 수 있습니다. `FAIL_FAST`는 첫 secondary 실패에서 중단하고,
`BEST_EFFORT`는 모든 secondary를 시도한 뒤 aggregate exception을 던집니다.

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
