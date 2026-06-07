package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventPublisher
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

/**
 * Publishes JaVers snapshot events through a vanilla Apache Kafka [Producer].
 *
 * ## Behavior / Contract
 * - Publishes to [options.topic].
 * - Uses [keyMapper] for the Kafka record key.
 * - Waits up to [VanillaKafkaCdoSnapshotRepositoryOptions.publishTimeout] for the send acknowledgement.
 * - Optionally flushes after a successful acknowledgement.
 * - Closes the producer only when [VanillaKafkaCdoSnapshotRepositoryOptions.closeProducerOnClose] is `true`.
 */
class VanillaKafkaSnapshotEventPublisher(
    private val producer: Producer<String, String>,
    private val options: VanillaKafkaCdoSnapshotRepositoryOptions,
    private val keyMapper: (CdoSnapshotEvent<String>) -> String = { it.metadata.globalIdValue },
): CdoSnapshotEventPublisher<String>, AutoCloseable {

    override fun publish(event: CdoSnapshotEvent<String>) {
        publish(event, keyMapper(event))
    }

    /**
     * Publishes [event] with an explicit Kafka key.
     */
    fun publish(event: CdoSnapshotEvent<String>, key: String) {
        val record = ProducerRecord(options.topic, key, event.payload)
        try {
            producer.send(record).get(options.publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
            if (options.flushAfterSend) {
                producer.flush()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for topic=${options.topic}, key=$key", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for topic=${options.topic}, key=$key", e)
        }
    }

    override fun close() {
        if (options.closeProducerOnClose) {
            producer.close(options.publishTimeout)
        }
    }
}
