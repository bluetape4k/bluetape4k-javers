package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventPublisher
import io.bluetape4k.support.requireNotBlank
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Spring Kafka [KafkaTemplate]를 통해 JaVers snapshot event를 publish합니다.
 *
 * ## 동작 / 계약
 * - [topic]이 설정되어 있으면 해당 topic으로 publish하고, 그렇지 않으면 [KafkaTemplate.sendDefault]로
 *   template default topic에 publish합니다.
 * - Kafka record key에는 [keyMapper]를 사용합니다.
 * - send acknowledgement를 [publishTimeout]까지 기다립니다.
 * - interrupted publish를 전파하기 전에 interrupt status를 복원합니다.
 */
class KafkaSnapshotEventPublisher private constructor(
    private val kafkaOperations: KafkaTemplate<String, String>,
    private val publishTimeout: Duration = Duration.ofSeconds(30),
    private val keyMapper: (CdoSnapshotEvent<String>) -> String = { it.metadata.globalIdValue },
    topic: String? = null,
): CdoSnapshotEventPublisher<String> {

    private val topic: String? = topic?.requireNotBlank("topic")

    companion object {
        /**
         * validation된 publish timeout으로 Spring Kafka snapshot event publisher를 생성합니다.
         */
        operator fun invoke(
            kafkaOperations: KafkaTemplate<String, String>,
            publishTimeout: Duration = Duration.ofSeconds(30),
            keyMapper: (CdoSnapshotEvent<String>) -> String = { it.metadata.globalIdValue },
        ): KafkaSnapshotEventPublisher =
            KafkaSnapshotEventPublisher(
                kafkaOperations = kafkaOperations,
                publishTimeout = publishTimeout.requirePositivePublishTimeout(),
                keyMapper = keyMapper,
            )

        /**
         * [topic]으로 전송하는 Spring Kafka snapshot event publisher를 생성합니다.
         */
        fun withTopic(
            kafkaOperations: KafkaTemplate<String, String>,
            topic: String,
            publishTimeout: Duration = Duration.ofSeconds(30),
            keyMapper: (CdoSnapshotEvent<String>) -> String = { it.metadata.globalIdValue },
        ): KafkaSnapshotEventPublisher =
            KafkaSnapshotEventPublisher(
                kafkaOperations = kafkaOperations,
                publishTimeout = publishTimeout.requirePositivePublishTimeout(),
                keyMapper = keyMapper,
                topic = topic,
            )
    }

    override fun publish(event: CdoSnapshotEvent<String>) {
        publish(event, keyMapper(event))
    }

    /**
     * 명시적 Kafka key로 [event]를 publish합니다.
     */
    fun publish(event: CdoSnapshotEvent<String>, key: String) {
        key.requireNotBlank("key")
        val keyDiagnostics = KafkaSnapshotKeyDiagnostics.format(key)

        try {
            val result = topic
                ?.let { kafkaOperations.send(it, key, event.payload) }
                ?: kafkaOperations.sendDefault(key, event.payload)

            result.get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for $keyDiagnostics", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for $keyDiagnostics", e)
        }
    }
}
