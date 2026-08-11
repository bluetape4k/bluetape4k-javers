package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.spring.publishAfterCommit
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireGt
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Spring Kafka [KafkaTemplate]로 domain event를 Kafka에 publish합니다.
 *
 * ## 계약
 * Spring transaction synchronization이 active이면 event를 `afterCommit`에서 보냅니다.
 * 그렇지 않으면 즉시 보냅니다. 두 경로 모두 Kafka acknowledgement를
 * [publishTimeout]까지 기다리며, 전송 실패·timeout·interrupt를 호출자에게 전파합니다.
 *
 * @property kafkaTemplate domain event 전송에 사용하는 template입니다.
 * @property topicResolver domain event를 Kafka topic으로 mapping합니다.
 * @property keyResolver domain event를 Kafka record key로 mapping합니다.
 * @property publishTimeout Kafka acknowledgement를 기다리는 최대 시간입니다.
 */
class KafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, DomainEvent>,
    private val topicResolver: (DomainEvent) -> String,
    private val keyResolver: (DomainEvent) -> String = { it.aggregateId.toString() },
): DomainEventPublisher {

    private var publishTimeout: Duration = DEFAULT_PUBLISH_TIMEOUT

    /** acknowledgement timeout을 지정하는 확장 생성자입니다. */
    constructor(
        kafkaTemplate: KafkaTemplate<String, DomainEvent>,
        topicResolver: (DomainEvent) -> String,
        keyResolver: (DomainEvent) -> String,
        publishTimeout: Duration,
    ): this(kafkaTemplate, topicResolver, keyResolver) {
        this.publishTimeout = publishTimeout.requirePositiveTimeout()
    }

    /** 기본 record key resolver를 사용하면서 acknowledgement timeout을 지정합니다. */
    constructor(
        kafkaTemplate: KafkaTemplate<String, DomainEvent>,
        topicResolver: (DomainEvent) -> String,
        publishTimeout: Duration,
    ): this(kafkaTemplate, topicResolver, { it.aggregateId.toString() }, publishTimeout)

    companion object: KLogging() {
        private val DEFAULT_PUBLISH_TIMEOUT: Duration = Duration.ofSeconds(30)
    }

    init {
        publishTimeout.requirePositiveTimeout()
    }

    override fun publish(event: DomainEvent) {
        publishAfterCommit {
            val topic = topicResolver(event)
            val key = keyResolver(event)
            try {
                kafkaTemplate.send(topic, key, event)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn(e) { "Kafka domain event publish interrupted. topic=$topic" }
                throw RuntimeException("Kafka domain event publish interrupted. topic=$topic", e)
            } catch (e: TimeoutException) {
                log.warn(e) { "Kafka domain event publish timed out. topic=$topic" }
                throw RuntimeException("Kafka domain event publish timed out. topic=$topic", e)
            } catch (e: ExecutionException) {
                val cause = e.cause ?: e
                log.warn(cause) { "Kafka domain event publish failed. topic=$topic" }
                throw RuntimeException("Kafka domain event publish failed. topic=$topic", cause)
            } catch (e: Exception) {
                log.warn(e) { "Kafka domain event publish failed. topic=$topic" }
                throw RuntimeException("Kafka domain event publish failed. topic=$topic", e)
            }
        }
    }
}

private fun Duration.requirePositiveTimeout(): Duration =
    requireGt(Duration.ZERO, "publishTimeout")
