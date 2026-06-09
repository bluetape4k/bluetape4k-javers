package io.bluetape4k.javers.autoconfigure

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.repository.api.JaversRepository
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Base Spring Boot auto-configuration that builds a default [Javers] bean.
 *
 * ## Contract
 * This phase runs after repository phases. It backs off when the application
 * provides its own [Javers] bean and customizes the default builder through
 * ordered [JaversBuilderCustomizer] beans.
 */
@AutoConfiguration(
    after = [
        JaversExposedRepositoryAutoConfiguration::class,
        JaversLettuceRepositoryAutoConfiguration::class,
        JaversRedissonRepositoryAutoConfiguration::class,
        JaversSpringKafkaRepositoryAutoConfiguration::class,
        JaversVanillaKafkaRepositoryAutoConfiguration::class,
    ]
)
@ConditionalOnClass(
    name = [
        "org.javers.core.Javers",
        "org.javers.core.JaversBuilder",
        "org.javers.repository.api.JaversRepository",
    ]
)
@ConditionalOnProperty(
    prefix = JaversAutoConfigurationProperties.PREFIX,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@ConditionalOnBean(type = ["org.javers.repository.api.JaversRepository"])
@ConditionalOnMissingBean(type = ["org.javers.core.Javers"])
@EnableConfigurationProperties(JaversAutoConfigurationProperties::class)
class JaversAutoConfiguration {

    /**
     * Creates the default JaVers instance backed by the selected repository.
     */
    @Bean
    fun javers(
        repository: JaversRepository,
        customizers: ObjectProvider<JaversBuilderCustomizer>,
    ): Javers {
        val builder = JaversBuilder.javers()
            .registerJaversRepository(repository)

        customizers.orderedStream().forEach { it.customize(builder) }
        return builder.build()
    }
}

/**
 * Callback for applications that need to customize the auto-configured [JaversBuilder].
 */
fun interface JaversBuilderCustomizer {

    /**
     * Applies application-specific registrations to [builder].
     */
    fun customize(builder: JaversBuilder)
}
