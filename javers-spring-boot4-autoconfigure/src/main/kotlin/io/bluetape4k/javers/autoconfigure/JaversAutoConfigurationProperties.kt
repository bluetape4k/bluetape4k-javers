package io.bluetape4k.javers.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.io.Serializable
import java.time.Duration

/**
 * bluetape4k JaVers Spring Boot auto-configuration을 위한 configuration properties입니다.
 *
 * ## 계약
 * auto-configuration은 기본적으로 전체 활성화되지만, [repository.type]이 구체적인
 * backend를 선택하지 않으면 repository를 만들지 않습니다. Exposed `Database`,
 * Redis client, Kafka producer 같은 infrastructure client는 application 소유
 * bean으로 남습니다.
 */
@ConfigurationProperties(prefix = JaversAutoConfigurationProperties.PREFIX)
data class JaversAutoConfigurationProperties(
    val enabled: Boolean = true,
    @field:NestedConfigurationProperty
    val repository: JaversRepositoryProperties = JaversRepositoryProperties(),
    @field:NestedConfigurationProperty
    val exposed: JaversExposedProperties = JaversExposedProperties(),
    @field:NestedConfigurationProperty
    val redis: JaversRedisProperties = JaversRedisProperties(),
    @field:NestedConfigurationProperty
    val kafka: JaversKafkaProperties = JaversKafkaProperties(),
): Serializable {

    companion object {
        const val PREFIX: String = "bluetape4k.javers"
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JaVers repository auto-configuration의 backend 선택 properties입니다.
 */
data class JaversRepositoryProperties(
    val type: JaversRepositoryType = JaversRepositoryType.NONE,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * bluetape4k JaVers auto-configuration이 지원하는 repository backend 유형입니다.
 */
enum class JaversRepositoryType {
    /** repository를 자동으로 만들지 않습니다. */
    NONE,

    /** 기존 Exposed `Database` bean에서 Exposed JDBC repository를 만듭니다. */
    EXPOSED,

    /** 기존 `RedisClient` bean에서 Lettuce Redis repository를 만듭니다. */
    LETTUCE,

    /** 기존 `RedissonClient` bean에서 Redisson Redis repository를 만듭니다. */
    REDISSON,

    /** 기존 `KafkaTemplate` bean에서 Spring Kafka write-only repository를 만듭니다. */
    SPRING_KAFKA,

    /** 기존 Apache Kafka `Producer` bean에서 vanilla Kafka write-only repository를 만듭니다. */
    VANILLA_KAFKA,
}

/**
 * Exposed JDBC repository properties입니다.
 */
data class JaversExposedProperties(
    val initializeSchema: Boolean = false,
    val createSchemaOnEnsure: Boolean = false,
    val commitTableName: String = "javers_commit",
    val snapshotTableName: String = "javers_snapshot",
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Lettuce와 Redisson backend가 공유하는 Redis repository properties입니다.
 */
data class JaversRedisProperties(
    val name: String = "default",
    val codec: JaversRedisCodec = JaversRedisCodec.LZ4_FORY,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * auto-configuration이 노출하는 안전한 Redis codec 선택지입니다.
 */
enum class JaversRedisCodec {
    /** LZ4로 압축한 Fory binary codec입니다. */
    LZ4_FORY,
}

/**
 * Spring Kafka와 vanilla Kafka backend가 공유하는 Kafka repository properties입니다.
 */
data class JaversKafkaProperties(
    val publishTimeout: Duration = Duration.ofSeconds(30),
    val topic: String = "javers-snapshots",
    val flushAfterSend: Boolean = false,
    val closeProducerOnClose: Boolean = false,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
