package io.bluetape4k.javers.examples.exposedddd.messaging

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.testcontainers.mq.KafkaServer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/**
 * [KafkaServer]를 사용해 주문 publisher의 broker acknowledgement와 record 전달을 검증합니다.
 */
class OrderKafkaEventPublisherIntegrationTest {

    @Test
    fun `publisher waits for acknowledgement and delivers order key and payload`() {
        val kafka = KafkaServer.Launcher.kafka
        val topic = "javers-exposed-ddd-orders-${Base58.randomString(8)}"
        val event = OrderPlaced(
            aggregateId = OrderId("order-kafka-integration"),
            occurredOn = Instant.parse("2026-05-27T00:00:00Z"),
            customerId = CustomerId("customer-kafka-integration"),
            totalAmount = "42.00",
        )

        KafkaServer.Launcher.createStringProducer(kafka).use { producer ->
            KafkaServer.Launcher.createStringConsumer(kafka).use { consumer ->
                consumer.subscribe(listOf(topic))

                OrderKafkaEventPublisher(producer, topic).publish(event)

                val record = awaitRecord(consumer, topic)
                record.key() shouldBeEqualTo event.aggregateId.value
                OrderDomainEventJsonCodec().decode(record.value()) shouldBeEqualTo event
            }
        }
    }

    private fun awaitRecord(
        consumer: KafkaConsumer<String, String>,
        topic: String,
    ): ConsumerRecord<String, String> {
        repeat(40) {
            consumer.poll(Duration.ofMillis(250))
                .firstOrNull { it.topic() == topic }
                ?.let { return it }
        }
        error("Timed out waiting for Kafka record on topic=$topic")
    }
}
