# Module bluetape4k-javers-persistence-redis

[English](./README.md) | 한국어

Redis를 JaVers CDO snapshot 저장소로 사용하기 위한 repository 모듈입니다.
기본 JaVers store 대신 Redis에 audit history를 저장하고 싶을 때 사용합니다.

## 아키텍처

![Redis persistence sequence](docs/images/readme-diagrams/javers-redis-sequence-01.png)

## 기능

- Lettuce 기반 `LettuceCdoSnapshotRepository`.
- Redisson 기반 `RedissonCdoSnapshotRepository`.
- JaVers global id별 encoded snapshot 저장.
- repository instance rebuild 후에도 head를 복원할 수 있는 commit id sequence 추적.
- `javers-core`의 shared codec 지원.

## 사용 예

```kotlin
val repository = LettuceCdoSnapshotRepository(
    name = "orders",
    client = redisClient,
)

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

Redis를 audit snapshot store로 사용할 때 이 모듈을 사용하세요. CQRS read model은
JaVers snapshot data와 분리된 application projection으로 유지하는 편이 좋습니다.

## Repository 선택 기준

두 repository는 같은 JaVers snapshot contract를 공유합니다. Snapshot은 최신순으로
조회되고, commit id sequence metadata를 저장해 repository head를 복원할 수 있으며,
codec 실패는 head commit을 전진시키지 않고 호출자에게 전파됩니다.

애플리케이션이 이미 Lettuce를 표준으로 쓰거나, Redis command/transaction 제어를
명시적으로 다루고 싶거나, JaVers audit store 주변에 가장 얇은 Redis client 계층을
두고 싶다면 `LettuceCdoSnapshotRepository`를 선택하세요.

애플리케이션이 이미 Redisson을 표준으로 쓰거나, 고수준 Redis collection을 활용하거나,
후속 layer에서 Redisson near-cache 및 read/write-through 전략을 평가하려면
`RedissonCdoSnapshotRepository`를 선택하세요.

이 모듈은 별도의 provider-neutral cache contract를 정의하지 않습니다. Near-cache,
read-through, write-through, write-behind 동작은 JaVers 전용 mapping code를 추가하기
전에 기존 bluetape4k cache 및 Exposed cache 모듈 재사용을 우선합니다.

Exposed가 durable audit store라면 canonical snapshot history는 `javers-exposed`에
맡기고, Redisson/Lettuce cache 전략은 `bluetape4k-exposed`를 통해 read model 또는
projection에 적용하세요. 이 Redis repository를 SQL-backed audit write, commit
sequence metadata, repository head 복원을 위한 write-behind cache로 사용하지 마세요.

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-redis")
}
```

## 빌드

```bash
./gradlew :javers-persistence-redis:test
```

## 참고 자료

- [JaVers](https://javers.org)
- [Redis](https://redis.io/)
