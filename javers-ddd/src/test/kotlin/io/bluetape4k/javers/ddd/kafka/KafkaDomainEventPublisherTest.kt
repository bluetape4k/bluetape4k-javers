package io.bluetape4k.javers.ddd.kafka

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class KafkaDomainEventPublisherTest {

    @Test
    fun `publisher sends event to resolved topic and key`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val sendResult = mockk<SendResult<String, DomainEvent>>()
        val event = TestEvent(aggregateId = "order-1")
        every {
            kafkaTemplate.send("domain-events", "order-1", event)
        } returns CompletableFuture.completedFuture(sendResult)

        KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
        ).publish(event)

        verify(exactly = 1) { kafkaTemplate.send("domain-events", "order-1", event) }
        confirmVerified(kafkaTemplate)
    }

    @Test
    fun `publisher propagates Kafka send failure`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val failure = IllegalStateException("broker unavailable")
        val event = TestEvent(aggregateId = "order-1")
        every {
            kafkaTemplate.send("domain-events", "order-1", event)
        } returns CompletableFuture.failedFuture(failure)

        val thrown = assertFailsWith<RuntimeException> {
            KafkaDomainEventPublisher(
                kafkaTemplate = kafkaTemplate,
                topicResolver = { "domain-events" },
            ).publish(event)
        }

        thrown.cause shouldBeSameInstanceAs failure
    }

    @Test
    fun `publisher propagates Kafka send timeout`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val event = TestEvent(aggregateId = "order-timeout")
        val timeoutFuture = object : CompletableFuture<SendResult<String, DomainEvent>>() {
            override fun get(timeout: Long, unit: TimeUnit): SendResult<String, DomainEvent> =
                throw TimeoutException("timed out")
        }
        every {
            kafkaTemplate.send("domain-events", "order-timeout", event)
        } returns timeoutFuture

        val thrown = assertFailsWith<RuntimeException> {
            KafkaDomainEventPublisher(
                kafkaTemplate = kafkaTemplate,
                topicResolver = { "domain-events" },
                publishTimeout = Duration.ofSeconds(1),
            ).publish(event)
        }

        thrown.cause shouldBeInstanceOf TimeoutException::class
    }

    @Test
    fun `publisher restores interrupt status when Kafka send is interrupted`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val event = TestEvent(aggregateId = "order-interrupted")
        val sendStarted = CountDownLatch(1)
        val pending = CompletableFuture<SendResult<String, DomainEvent>>()
        val failure = AtomicReference<Throwable?>()
        every {
            kafkaTemplate.send("domain-events", "order-interrupted", event)
        } answers {
            sendStarted.countDown()
            pending
        }
        val publisher = KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
            publishTimeout = Duration.ofSeconds(5),
        )
        val thread = Thread {
            try {
                publisher.publish(event)
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }

        try {
            thread.start()
            sendStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
            thread.interrupt()
            thread.join(1_000)

            thread.isAlive.shouldBeFalse()
            thread.isInterrupted.shouldBeTrue()
            val thrown = failure.get().shouldNotBeNull()
            thrown.cause shouldBeInstanceOf InterruptedException::class
        } finally {
            pending.cancel(true)
        }
    }

    @Test
    fun `publisher rejects non-positive publish timeout`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()

        assertFailsWith<IllegalArgumentException> {
            KafkaDomainEventPublisher(
                kafkaTemplate = kafkaTemplate,
                topicResolver = { "domain-events" },
                publishTimeout = Duration.ZERO,
            )
        }
    }

    @Test
    fun `publisher sends only after transaction commit`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val transactionManager = LocalTransactionManager()
        val sendResult = mockk<SendResult<String, DomainEvent>>()
        val event = TestEvent(aggregateId = "order-commit")
        every {
            kafkaTemplate.send("domain-events", "order-commit", event)
        } answers {
            transactionManager.phases += "send"
            CompletableFuture.completedFuture(sendResult)
        }
        val publisher = KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
        )
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.executeWithoutResult {
            publisher.publish(event)
            verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }
        }

        verify(exactly = 1) { kafkaTemplate.send("domain-events", "order-commit", event) }
        transactionManager.phases shouldBeEqualTo listOf("begin", "commit", "send")
        transactionManager.commitCount shouldBeEqualTo 1
        transactionManager.rollbackCount shouldBeEqualTo 0
    }

    @Test
    fun `publisher does not send after transaction rollback`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val transactionManager = LocalTransactionManager()
        val event = TestEvent(aggregateId = "order-rollback")
        val publisher = KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
        )
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.executeWithoutResult { status ->
            publisher.publish(event)
            status.setRollbackOnly()
        }

        verify(exactly = 0) { kafkaTemplate.send(any(), any(), any()) }
        confirmVerified(kafkaTemplate)
        transactionManager.phases shouldBeEqualTo listOf("begin", "rollback")
        transactionManager.commitCount shouldBeEqualTo 0
        transactionManager.rollbackCount shouldBeEqualTo 1
    }

    @Test
    fun `publisher propagates Kafka failure from transaction commit`() {
        val kafkaTemplate = mockk<KafkaTemplate<String, DomainEvent>>()
        val transactionManager = LocalTransactionManager()
        val failure = IllegalStateException("broker unavailable")
        val event = TestEvent(aggregateId = "order-commit-failure")
        every {
            kafkaTemplate.send("domain-events", "order-commit-failure", event)
        } answers {
            transactionManager.phases += "send"
            CompletableFuture.failedFuture(failure)
        }
        val publisher = KafkaDomainEventPublisher(
            kafkaTemplate = kafkaTemplate,
            topicResolver = { "domain-events" },
        )
        val transactionTemplate = TransactionTemplate(transactionManager)

        val thrown = assertFailsWith<RuntimeException> {
            transactionTemplate.executeWithoutResult {
                publisher.publish(event)
            }
        }

        thrown.cause shouldBeSameInstanceAs failure
        transactionManager.phases shouldBeEqualTo listOf("begin", "commit", "send")
        transactionManager.commitCount shouldBeEqualTo 1
        transactionManager.rollbackCount shouldBeEqualTo 0
    }

    private class LocalTransactionManager: AbstractPlatformTransactionManager() {
        val phases = mutableListOf<String>()
        var commitCount = 0
        var rollbackCount = 0

        override fun doGetTransaction(): Any = Any()

        override fun doBegin(transaction: Any, definition: TransactionDefinition) {
            phases += "begin"
        }

        override fun doCommit(status: DefaultTransactionStatus) {
            commitCount++
            phases += "commit"
        }

        override fun doRollback(status: DefaultTransactionStatus) {
            rollbackCount++
            phases += "rollback"
        }
    }

    data class TestEvent(
        override val aggregateId: String,
        override val occurredOn: Instant = Instant.parse("2026-05-26T00:00:00Z"),
    ): DomainEvent
}
