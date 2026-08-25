# javers-spring-boot4-autoconfigure

English | [한국어](./README.ko.md)

Spring Boot 4 auto-configuration for bluetape4k JaVers repositories.

## Overview

`javers-spring-boot4-autoconfigure` creates a JaVers repository and a default
`Javers` bean when an application selects one supported backend. The module
keeps infrastructure ownership in the application: it never creates Exposed
`Database`, Redis client, Kafka producer, or Spring Kafka `KafkaTemplate` beans.

## Supported Backends

| Property value | Required application bean | Repository |
|---|---|---|
| `exposed` | `org.jetbrains.exposed.v1.jdbc.Database` | `ExposedCdoSnapshotRepository` |
| `lettuce` | `io.lettuce.core.RedisClient` | `LettuceCdoSnapshotRepository` |
| `redisson` | `org.redisson.api.RedissonClient` | `RedissonCdoSnapshotRepository` |
| `spring-kafka` | `org.springframework.kafka.core.KafkaTemplate<String, String>` | `KafkaCdoSnapshotRepository` |
| `vanilla-kafka` | `org.apache.kafka.clients.producer.Producer<String, String>` | `VanillaKafkaCdoSnapshotRepository` |
| `none` | none | no repository |

Redis auto-configuration exposes only the safe `lz4-fory` codec path and backs
off when the codec classes are missing.

## Dependency

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:0.3.0"))
    implementation("io.github.bluetape4k.javers:javers-spring-boot4-autoconfigure")

    // Add only the backend module and client stack your application uses.
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

## Exposed Example

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

With that setup, Spring Boot registers `JaversRepository` and `Javers` beans
unless the application already provides them.

Schema creation is opt-in. Keep `initialize-schema` and
`create-schema-on-ensure` disabled when schema ownership belongs to migrations.

### Exposed schema flag combinations

The two flags separate schema ownership from creation timing:

| `initialize-schema` | `create-schema-on-ensure` | Meaning |
|---:|---:|---|
| `false` | `false` | Default. Flyway, Liquibase, or Exposed migrations own the tables; auto-configuration runs no DDL. |
| `false` | `true` | DDL is allowed only when application code calls repository `ensureSchema()`. |
| `true` | `true` | Auto-configuration calls `ensureSchema()` while creating the repository bean, so tables are created at startup. |
| `true` | `false` | Contradictory. The Exposed backend fails fast because `ensureSchema()` is a no-op and cannot satisfy initialization. |

`initialize-schema=true` requires `create-schema-on-ensure=true`. Keep both
disabled for production schemas owned by migration tooling.

## Redis Example

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

Use `type: redisson` with a `RedissonClient` bean for the Redisson-backed path.

## Kafka Example

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

Kafka repositories are write-only event stream adapters. Query methods return
empty results by design; use a projection or another repository for read-side
audit queries.

`bluetape4k.javers.kafka.topic` is used by both `spring-kafka` and
`vanilla-kafka` backends. For Spring Kafka, the repository sends to this topic
directly instead of relying on a `KafkaTemplate` default topic.

## Customizing JaVers

Add ordered `JaversBuilderCustomizer` beans when the default builder needs
entity registrations or other JaVers options:

```kotlin
@Bean
fun orderAuditCustomizer(): JaversBuilderCustomizer =
    JaversBuilderCustomizer { builder ->
        builder.registerEntity(Order::class.java)
    }
```

## Configuration Properties

| Property | Default | Description |
|---|---|---|
| `bluetape4k.javers.enabled` | `true` | Enables all auto-configuration phases. |
| `bluetape4k.javers.repository.type` | `none` | Repository backend to create. |
| `bluetape4k.javers.exposed.initialize-schema` | `false` | Calls `ensureSchema()` after creating the Exposed repository when explicitly enabled; requires `create-schema-on-ensure=true`. |
| `bluetape4k.javers.exposed.create-schema-on-ensure` | `false` | Allows `ensureSchema()` to create missing tables when explicitly enabled. |
| `bluetape4k.javers.exposed.commit-table-name` | `javers_commit` | Commit table name for the Exposed repository. |
| `bluetape4k.javers.exposed.snapshot-table-name` | `javers_snapshot` | Snapshot table name for the Exposed repository. |
| `bluetape4k.javers.redis.name` | `default` | Redis key namespace. |
| `bluetape4k.javers.redis.codec` | `lz4-fory` | Redis binary codec. |
| `bluetape4k.javers.kafka.topic` | `javers-snapshots` | Topic for Spring Kafka and vanilla Kafka repository events. |
| `bluetape4k.javers.kafka.publish-timeout` | `30s` | Kafka publish timeout. |
| `bluetape4k.javers.kafka.flush-after-send` | `false` | Flushes vanilla Kafka producer after each send. |
| `bluetape4k.javers.kafka.close-producer-on-close` | `false` | Closes the application-provided vanilla Kafka producer when the repository closes. |
