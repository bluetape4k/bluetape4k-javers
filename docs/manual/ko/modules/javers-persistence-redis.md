# javers-persistence-redis

`javers-persistence-redis`는 Lettuce와 Redisson용 `CdoSnapshotRepository`를 각각 제공합니다. Redis를 감사 스냅샷의 실제 저장소로 쓰고, Redis의 내구성·캐시 축출·메모리 정책까지 감사 계약으로 관리할 때 선택하세요.

## 의존성과 클라이언트 선택

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-persistence-redis")
    implementation("io.github.bluetape4k:bluetape4k-lettuce") // 둘 중 하나
    // implementation("org.redisson:redisson")
}
```

Redis 연동은 모듈 빌드에서 선택 의존성이므로 애플리케이션이 사용할 구현을 추가해야 합니다. Lettuce 저장소는 `bluetape4k-lettuce`의 도우미를 호출하며, 이 모듈이 Lettuce 클라이언트 의존성도 제공합니다. `lettuce-core`만 추가해서는 실행할 수 없습니다. Lettuce 구현은 전용 동기 연결을 두고 스냅샷을 쓸 때마다 잠금 안에서 `MULTI/EXEC`를 실행합니다. Redisson 구현은 스냅샷에 `RListMultimap`, 커밋 순서에 `RMap`을 사용합니다. 구현이 둘 다 있다고 두 클라이언트를 함께 넣을 이유는 없습니다.

## Lettuce 시작 예제

```kotlin
import io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository
import io.lettuce.core.RedisClient
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id

data class Order(@Id val id: Long, val status: String)

val redisClient = RedisClient.create("redis://localhost:6379")
try {
    val repository = LettuceCdoSnapshotRepository("orders", redisClient)
    val javers = JaversBuilder.javers()
        .registerJaversRepository(repository)
        .registerEntity(Order::class.java)
        .build()

    javers.commit("order-service", Order(1, "PLACED"))
} finally {
    redisClient.shutdown()
}
```

Lettuce는 최신 스냅샷이 앞에 오도록 `javers:{name}:snapshot:{globalId}` 목록에 바이트 배열을 저장합니다. GlobalId 색인은 `javers:{name}:globalId:set`, 커밋 순서는 `javers:{name}:sequence:set`에 둡니다. 스냅샷 목록 추가와 GlobalId 색인 갱신은 Redis 트랜잭션 하나로 묶지만, 상위 저장 흐름이 나중에 수행하는 순서 갱신은 별도입니다. 정확한 동작은 [`LettuceCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt)에 있습니다.

## Redisson 시작 예제

```kotlin
import io.bluetape4k.javers.persistence.redis.repository.RedissonCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.redisson.Redisson
import org.redisson.config.Config

data class Order(@Id val id: Long, val status: String)

val config = Config().apply {
    useSingleServer().address = "redis://127.0.0.1:6379"
}
val redisson = Redisson.create(config)
try {
    val repository = RedissonCdoSnapshotRepository("orders", redisson)
    val javers = JaversBuilder.javers()
        .registerJaversRepository(repository)
        .registerEntity(Order::class.java)
        .build()

    javers.commit("order-service", Order(1, "PLACED"))
} finally {
    redisson.shutdown()
}
```

Redisson은 GlobalId별 스냅샷 목록을 `javers:{name}:snapshot`, 커밋 순서를 `javers:{name}:sequence`에 저장합니다. 스냅샷 추가와 나중의 순서 갱신은 서로 다른 원격 작업입니다. 구현은 [`RedissonCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt)에서 확인하세요.

두 저장소 모두 기본 LZ4/Fory 코덱을 쓰고 최신 스냅샷부터 반환합니다. 저장소를 다시 만들면 순서 맵을 훑어 최신 커밋을 복원합니다.

선택한 클라이언트는 저장소와 수명을 같이하고, 서비스가 JaVers 사용을 끝낸 뒤에 닫아야 합니다. 위 `finally`는 짧게 실행하는 독립 프로그램의 자원 소유권을 보여 줍니다. 의존성 주입 컨테이너를 쓴다면 애플리케이션 종료 단계에서 클라이언트를 닫으세요.

## 실패와 운영

Lettuce는 트랜잭션 오류를 호출자에게 전달하고 `DISCARD`도 시도합니다. 다만 네트워크가 끊기면 명령이 Redis에 적용됐는지 클라이언트가 확신하지 못할 수 있습니다. Redisson도 스냅샷 추가와 순서 갱신을 한 트랜잭션으로 묶지 않습니다. 두 구현 모두 감사 상태가 일부만 남을 수 있고, 커밋 전체를 재시도하면 스냅샷이 중복으로 들어갈 수 있습니다.

디코딩은 `mapNotNull`을 사용하므로 손상됐거나 호환되지 않는 바이트 배열이 조회 결과에서 빠질 수 있습니다. Redis의 캐시 축출은 스냅샷, 색인, 순서 값을 서로 따로 지울 수도 있습니다. 필요한 감사 보존 기간에 맞춰 영속성, 축출, 복제, 백업, 키 공간 경고, 여유 메모리, 코덱 업그레이드 테스트를 정하세요. 환경과 바운디드 컨텍스트마다 의도적으로 다른 `name`을 써서 키 충돌을 막습니다.

## 테스트

```bash
./gradlew :javers-persistence-redis:test
```

릴리스는 Redis Testcontainers에서 Lettuce와 Redisson의 커밋·섀도 테스트를 각각 실행합니다. 저장소 재생성 뒤 최신 커밋 복원은 [`LettuceJaversCommitTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceJaversCommitTest.kt)와 [`RedissonJaversCommitTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonJaversCommitTest.kt)가 검증합니다.

## 하지 않는 일

- CQRS 프로젝션 API가 아닙니다.
- 추가 전용 이벤트 로그가 아닙니다.
- 여러 키나 커밋 전체의 exactly-once 처리를 보장하지 않습니다.
- Redis 영속성, 캐시 축출, 클러스터, 백업 정책을 정하지 않습니다.

이어서 [Redis 영속 저장](../persistence/redis.md), [실패 계약](../operations/failure-contracts.md), [관측성](../operations/observability.md)을 읽어 보세요.
