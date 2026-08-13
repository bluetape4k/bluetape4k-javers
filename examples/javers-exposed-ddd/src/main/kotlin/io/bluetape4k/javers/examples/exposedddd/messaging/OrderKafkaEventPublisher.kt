package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * query-side projection을 위해 주문 domain event를 Kafka로 발행합니다.
 *
 * ## 계약
 * 이 예제 publisher는 동기 방식이며 fail-fast로 동작합니다. Kafka 전송 실패는
 * [DomainEventPublisher]를 통해 command handler 실패로 전파됩니다. Kafka acknowledgement를
 * 30초까지 기다리며, timeout·producer failure·interrupt를 구분해 재전파합니다. interrupt가
 * 발생하면 현재 thread의 interrupt 상태를 복구하며, 오류 진단에는 topic만 포함하고 event
 * payload와 key는 노출하지 않습니다.
 */
class OrderKafkaEventPublisher(
    private val producer: Producer<String, String>,
    private val topic: String,
    private val codec: OrderDomainEventJsonCodec = OrderDomainEventJsonCodec(),
): DomainEventPublisher {

    companion object: KLogging()

    override fun publish(event: DomainEvent) {
        val key = (event.aggregateId as OrderId).value
        try {
            producer.send(ProducerRecord(topic, key, codec.encode(event))).get(30, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn(e) { "Kafka order event publish interrupted. topic=$topic" }
            throw RuntimeException("Kafka order event publish interrupted. topic=$topic", e)
        } catch (e: TimeoutException) {
            log.warn(e) { "Kafka order event publish timed out. topic=$topic" }
            throw RuntimeException("Kafka order event publish timed out. topic=$topic", e)
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            log.warn(cause) { "Kafka order event publish failed. topic=$topic" }
            throw RuntimeException("Kafka order event publish failed. topic=$topic", cause)
        } catch (e: Exception) {
            log.warn(e) { "Kafka order event publish failed. topic=$topic" }
            throw RuntimeException("Kafka order event publish failed. topic=$topic", e)
        }
    }
}
