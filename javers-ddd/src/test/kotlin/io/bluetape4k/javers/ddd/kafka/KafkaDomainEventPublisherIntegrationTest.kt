package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.testcontainers.mq.KafkaServer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

/**
 * [KafkaServer]를 사용해 Kafka broker acknowledgement와 record 전달을 검증합니다.
 */
class KafkaDomainEventPublisherIntegrationTest {

    @Test
    fun `publisher waits for broker acknowledgement and delivers event record`() {
        val kafka = KafkaServer.Launcher.kafka
        val topic = "javers-ddd-domain-events-${Base58.randomString(8)}"
        val producerProperties = KafkaServer.Launcher.getProducerProperties(kafka)
            .mapValues { (_, value) -> requireNotNull(value) }
            .toMutableMap()
        val producerFactory = DefaultKafkaProducerFactory<String, DomainEvent>(
            producerProperties,
            StringSerializer(),
            DomainEventSerializer(),
        )
        val kafkaTemplate = KafkaTemplate(producerFactory, true)
        val event = TestEvent(aggregateId = "order-kafka-integration")

        KafkaServer.Launcher.createStringConsumer(kafka).use { consumer ->
            consumer.subscribe(listOf(topic))
            try {
                KafkaDomainEventPublisher(
                    kafkaTemplate = kafkaTemplate,
                    topicResolver = { topic },
                    publishTimeout = Duration.ofSeconds(10),
                ).publish(event)

                val record = awaitRecord(consumer, topic)
                record.key() shouldBeEqualTo event.aggregateId
                record.value() shouldBeEqualTo "${event.aggregateId}|${event.occurredOn}"
            } finally {
                kafkaTemplate.destroy()
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

    private class DomainEventSerializer: Serializer<DomainEvent> {
        override fun serialize(topic: String?, data: DomainEvent?): ByteArray? {
            return data?.let {
                "${it.aggregateId}|${it.occurredOn}".toByteArray(StandardCharsets.UTF_8)
            }
        }
    }

    private data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
