package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventPublisher
import io.bluetape4k.support.requireNotBlank
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Publishes JaVers snapshot events through a Spring Kafka [KafkaTemplate].
 *
 * ## Behavior / Contract
 * - Publishes to the template default topic with [KafkaTemplate.sendDefault].
 * - Uses [keyMapper] for the Kafka record key.
 * - Waits up to [publishTimeout] for the send acknowledgement.
 * - Restores interrupt status before propagating an interrupted publish.
 */
class KafkaSnapshotEventPublisher private constructor(
    private val kafkaOperations: KafkaTemplate<String, String>,
    private val publishTimeout: Duration = Duration.ofSeconds(30),
    private val keyMapper: (CdoSnapshotEvent<String>) -> String = { it.metadata.globalIdValue },
): CdoSnapshotEventPublisher<String> {

    companion object {
        /**
         * Creates a Spring Kafka snapshot event publisher with validated publish timeout.
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
    }

    override fun publish(event: CdoSnapshotEvent<String>) {
        publish(event, keyMapper(event))
    }

    /**
     * Publishes [event] with an explicit Kafka key.
     */
    fun publish(event: CdoSnapshotEvent<String>, key: String) {
        key.requireNotBlank("key")

        try {
            kafkaOperations.sendDefault(key, event.payload).get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for key=$key", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for key=$key", e)
        }
    }
}
