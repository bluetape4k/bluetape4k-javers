package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

/**
 * query-side projection을 위해 주문 domain event를 Kafka로 발행합니다.
 *
 * ## 계약
 * 이 예제 publisher는 동기 방식이며 fail-fast로 동작합니다. Kafka 전송 실패는
 * [DomainEventPublisher]를 통해 command handler 실패로 전파됩니다.
 */
class OrderKafkaEventPublisher(
    private val producer: Producer<String, String>,
    private val topic: String,
    private val codec: OrderDomainEventJsonCodec = OrderDomainEventJsonCodec(),
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        val key = (event.aggregateId as OrderId).value
        producer.send(ProducerRecord(topic, key, codec.encode(event))).get(30, TimeUnit.SECONDS)
    }
}
