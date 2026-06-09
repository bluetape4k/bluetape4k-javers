# javers-spring-boot4-autoconfigure

[English](./README.md) | 한국어

bluetape4k JaVers repository를 위한 Spring Boot 4 auto-configuration 모듈입니다.

## 개요

`javers-spring-boot4-autoconfigure`는 애플리케이션이 지원 backend를 선택하면
JaVers repository와 기본 `Javers` bean을 등록합니다. Exposed `Database`, Redis
client, Kafka producer, Spring Kafka `KafkaTemplate` 같은 인프라 bean은
애플리케이션이 소유합니다. 이 모듈은 그런 bean을 직접 생성하지 않습니다.

## 지원 Backend

| Property 값 | 필요한 애플리케이션 bean | Repository |
|---|---|---|
| `exposed` | `org.jetbrains.exposed.v1.jdbc.Database` | `ExposedCdoSnapshotRepository` |
| `lettuce` | `io.lettuce.core.RedisClient` | `LettuceCdoSnapshotRepository` |
| `redisson` | `org.redisson.api.RedissonClient` | `RedissonCdoSnapshotRepository` |
| `spring-kafka` | `org.springframework.kafka.core.KafkaTemplate<String, String>` | `KafkaCdoSnapshotRepository` |
| `vanilla-kafka` | `org.apache.kafka.clients.producer.Producer<String, String>` | `VanillaKafkaCdoSnapshotRepository` |
| `none` | 없음 | repository를 만들지 않음 |

Redis auto-configuration은 안전한 `lz4-fory` codec 경로만 노출하며, codec class가
없으면 조용히 backing off 합니다.

## 의존성

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:0.3.0"))
    implementation("io.github.bluetape4k.javers:javers-spring-boot4-autoconfigure")

    // 애플리케이션이 실제로 쓰는 backend module과 client stack만 추가하세요.
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

## Exposed 예제

```yaml
bluetape4k:
  javers:
    repository:
      type: exposed
    exposed:
      initialize-schema: false
      create-schema-on-ensure: false
      commit-table-name: javers_commit
      snapshot-table-name: javers_snapshot
```

```kotlin
@Configuration(proxyBeanMethods = false)
class AuditDatabaseConfiguration {

    @Bean
    fun auditDatabase(dataSource: DataSource): Database =
        Database.connect(dataSource)
}
```

이 설정이면 애플리케이션이 직접 `JaversRepository`나 `Javers`를 제공하지 않는 한
Spring Boot가 두 bean을 등록합니다.

Schema 생성은 opt-in입니다. Schema ownership이 migration에 있다면
`initialize-schema`와 `create-schema-on-ensure`를 끈 상태로 유지하세요.

## Redis 예제

```yaml
bluetape4k:
  javers:
    repository:
      type: lettuce
    redis:
      name: orders-audit
      codec: lz4-fory
```

```kotlin
@Configuration(proxyBeanMethods = false)
class AuditRedisConfiguration {

    @Bean
    fun redisClient(): RedisClient =
        RedisClient.create("redis://localhost:6379")
}
```

Redisson 경로는 `type: redisson`과 `RedissonClient` bean을 사용하세요.

## Kafka 예제

```yaml
bluetape4k:
  javers:
    repository:
      type: vanilla-kafka
    kafka:
      topic: javers-snapshots
      publish-timeout: 30s
      flush-after-send: false
      close-producer-on-close: false
```

Kafka repository는 write-only event stream adapter입니다. Query method는 설계상
빈 결과를 반환하므로, read-side audit query에는 projection이나 다른 repository를
사용하세요.

`bluetape4k.javers.kafka.topic`은 `spring-kafka`와 `vanilla-kafka` backend 모두에
적용됩니다. Spring Kafka repository는 `KafkaTemplate` default topic에 의존하지
않고 이 topic으로 직접 전송합니다.

## JaVers Customizer

기본 builder에 entity registration이나 JaVers option을 추가해야 하면 ordered
`JaversBuilderCustomizer` bean을 등록하세요:

```kotlin
@Bean
fun orderAuditCustomizer(): JaversBuilderCustomizer =
    JaversBuilderCustomizer { builder ->
        builder.registerEntity(Order::class.java)
    }
```

## Configuration Properties

| Property | 기본값 | 설명 |
|---|---|---|
| `bluetape4k.javers.enabled` | `true` | 전체 auto-configuration phase를 켭니다. |
| `bluetape4k.javers.repository.type` | `none` | 생성할 repository backend입니다. |
| `bluetape4k.javers.exposed.initialize-schema` | `false` | 명시적으로 켜면 Exposed repository 생성 후 `ensureSchema()`를 호출합니다. |
| `bluetape4k.javers.exposed.create-schema-on-ensure` | `false` | 명시적으로 켜면 `ensureSchema()`가 누락 table을 만들 수 있게 합니다. |
| `bluetape4k.javers.exposed.commit-table-name` | `javers_commit` | Exposed repository commit table 이름입니다. |
| `bluetape4k.javers.exposed.snapshot-table-name` | `javers_snapshot` | Exposed repository snapshot table 이름입니다. |
| `bluetape4k.javers.redis.name` | `default` | Redis key namespace입니다. |
| `bluetape4k.javers.redis.codec` | `lz4-fory` | Redis binary codec입니다. |
| `bluetape4k.javers.kafka.topic` | `javers-snapshots` | Spring Kafka와 vanilla Kafka repository event topic입니다. |
| `bluetape4k.javers.kafka.publish-timeout` | `30s` | Kafka publish timeout입니다. |
| `bluetape4k.javers.kafka.flush-after-send` | `false` | Vanilla Kafka producer를 send마다 flush합니다. |
| `bluetape4k.javers.kafka.close-producer-on-close` | `false` | Repository close 시 애플리케이션이 제공한 vanilla Kafka producer를 닫습니다. |
