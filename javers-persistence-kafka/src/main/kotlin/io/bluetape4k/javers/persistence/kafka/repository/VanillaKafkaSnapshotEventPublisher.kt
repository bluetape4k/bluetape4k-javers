package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.repository.event.CdoSnapshotEvent
import io.bluetape4k.javers.repository.event.CdoSnapshotEventPublisher
import io.bluetape4k.support.requireNotBlank
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

/**
 * vanilla Apache Kafka [Producer]를 통해 JaVers snapshot event를 publish합니다.
 *
 * ## 동작 / 계약
 * - [options.topic]으로 publish합니다.
 * - Kafka record key에는 [keyMapper]를 사용합니다.
 * - send acknowledgement를 [VanillaKafkaCdoSnapshotRepositoryOptions.publishTimeout]까지 기다립니다.
 * - acknowledgement가 성공하면 선택적으로 flush합니다.
 * - [VanillaKafkaCdoSnapshotRepositoryOptions.closeProducerOnClose]가 `true`일 때만 producer를 close합니다.
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
     * 명시적 Kafka key로 [event]를 publish합니다.
     */
    fun publish(event: CdoSnapshotEvent<String>, key: String) {
        key.requireNotBlank("key")
        val keyDiagnostics = KafkaSnapshotKeyDiagnostics.format(key)

        val record = ProducerRecord(options.topic, key, event.payload)
        try {
            producer.send(record).get(options.publishTimeout.toMillis(), TimeUnit.MILLISECONDS)
            if (options.flushAfterSend) {
                producer.flush()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted for topic=${options.topic}, $keyDiagnostics", e)
        } catch (e: Exception) {
            throw RuntimeException("Kafka publish failed for topic=${options.topic}, $keyDiagnostics", e)
        }
    }

    override fun close() {
        if (options.closeProducerOnClose) {
            producer.close(options.publishTimeout)
        }
    }
}
