# Module bluetape4k-javers-persistence-redis

[English](./README.md) | 한국어

Redis를 durable audit snapshot store로 사용하는 JaVers CDO snapshot repository
모듈입니다. 기본 JaVers store 대신 Redis에 audit history를 저장하고 싶을 때
사용합니다.

## Repository map

![JaVers Redis repository map](../docs/images/readme-diagrams/javers-redis-repository-map-01.png)

이 모듈은 같은 JaVers snapshot contract 위에 두 repository 구현을 제공합니다.
`LettuceCdoSnapshotRepository`는 Redis command와 명시적인 `MULTI`/`EXEC` write
경로에 가깝게 동작합니다. `RedissonCdoSnapshotRepository`는 Redisson collection을
사용하면서도 최신순 query, head 복원, codec 실패 경계를 같은 방식으로 유지합니다.

## Redis key layout

![JaVers Redis key layout](../docs/images/readme-diagrams/javers-redis-key-layout-01.png)

Lettuce 구현은 repository-scoped Redis hash에 global id와 commit sequence metadata를
저장하고, JaVers global id마다 snapshot list를 둡니다. Redisson 구현은 같은 audit
data를 snapshot multimap과 commit sequence map으로 저장합니다. 두 layout 모두
constructor의 `name`으로 repository-scoped 이름을 만들기 때문에 여러 audit store가
같은 Redis deployment를 공유해도 key 충돌을 피할 수 있습니다.

## Save and load flow

![JaVers Redis save and load flow](../docs/images/readme-diagrams/javers-redis-save-load-flow-01.png)

Snapshot write는 head metadata를 전진시키기 전에 설정된 `JaversCodec<ByteArray>`로
`CdoSnapshot`을 encode합니다. Read path는 JaVers global id로 encoded row를 읽고,
결과를 최신순으로 정규화한 뒤 bytes를 JaVers snapshot으로 decode합니다.

## 기능

- Lettuce 기반 `LettuceCdoSnapshotRepository`.
- Redisson 기반 `RedissonCdoSnapshotRepository`.
- JaVers global id별 encoded snapshot 저장.
- repository instance rebuild 후에도 head를 복원할 수 있는 commit id sequence 추적.
- `javers-core`의 `JaversCodecs.LZ4Fory` 기본값과 custom
  `JaversCodec<ByteArray>` 지원.

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

## 생명주기

`LettuceCdoSnapshotRepository.close()`는 terminal lifecycle입니다. repository가
연 read/write connection만 닫고, 이후 모든 read/write operation은
`IllegalStateException`으로 거부합니다. 호출자가 소유한 `RedisClient`는 종료하지
않으므로 repository를 먼저 닫은 뒤 client를 shutdown하세요.

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
