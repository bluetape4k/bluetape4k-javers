package io.bluetape4k.javers.ddd

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.exposed.core.ddd.AbstractAggregateRoot
import io.bluetape4k.exposed.core.ddd.DomainEvent as ExposedEvent
import io.mockk.every
import io.mockk.mockk
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CancellationException

class AggregateAuditAdapterTest {
    private fun javers(): Javers = JaversBuilder.javers().registerEntity(Order::class.java).build()
    private fun adapter(
        clear: (Order) -> Unit = { it.clearDomainEvents() },
        mapper: (ExposedEvent<Long>) -> DomainEvent = ::mapped,
    ) = AggregateAuditAdapter<Order, ExposedEvent<Long>>(Order::domainEvents, clear, mapper)

    @Test
    fun `Exposed 원본 aggregate와 metadata를 감사하고 실제 완료 후 clear한다`() {
        val order = Order().apply { emit() }
        val original = order.domainEvents().single()
        val registration = adapter().capture(order)
        val commit = registration.audit(javers(), "adapter")
        commit.snapshots.any { it.globalId.typeName == Order::class.java.name } shouldBeEqualTo true
        commit.properties["aggregateId"] shouldBeEqualTo "42"
        commit.properties["occurredOn"] shouldBeEqualTo original.occurredAt.toString()
        commit.properties["domainEventType"] shouldBeEqualTo original.javaClass.name
        commit.properties["event.tenant"] shouldBeEqualTo "blue"
        val published = mutableListOf<ExposedEvent<Long>>()
        registration.publish { published += it }
        order.domainEvents() shouldHaveSize 1
        registration.complete(AuditCompletion.COMMITTED)
        order.domainEvents() shouldHaveSize 0
        (published.single() === original) shouldBeEqualTo true
        registration.events shouldHaveSize 1
    }

    @Test
    fun `rollback과 unknown은 원본 이벤트를 보존한다`() {
        listOf(AuditCompletion.ROLLED_BACK, AuditCompletion.UNKNOWN).forEach { completion ->
            val order = Order().apply { emit() }
            val original = order.domainEvents().single()
            val registration = adapter(clear = { error("clear forbidden") }).capture(order)
            registration.audit(javers(), "adapter")
            registration.complete(completion)
            (order.domainEvents().single() === original) shouldBeEqualTo true
            assertFailsWith<IllegalStateException> { registration.publish { error("publish forbidden") } }
        }
    }

    @Test
    fun `순서 위반과 중복 호출은 부수효과를 실행하지 않는다`() {
        val order = Order().apply { emit() }
        var cleared = 0
        val registration = adapter(clear = { cleared++; it.clearDomainEvents() }).capture(order)
        assertFailsWith<IllegalStateException> { registration.publish { error("publish forbidden") } }
        assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
        registration.audit(javers(), "adapter")
        assertFailsWith<IllegalStateException> { registration.audit(javers(), "adapter") }
        var published = 0
        registration.publish { published++ }
        assertFailsWith<IllegalStateException> { registration.publish { published++ } }
        registration.complete(AuditCompletion.COMMITTED)
        assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
        published shouldBeEqualTo 1
        cleared shouldBeEqualTo 1
    }

    @Test
    fun `mapper와 audit 실패는 buffer를 보존하고 재사용을 거부한다`() {
        val order = Order().apply { emit() }
        val failedMapper = adapter(mapper = { throw IllegalArgumentException("mapper") }).capture(order)
        assertFailsWith<IllegalArgumentException> { failedMapper.audit(javers(), "adapter") }
        assertFailsWith<IllegalStateException> { failedMapper.publish {} }
        val failing = mockk<Javers>()
        every { failing.commit(any<String>(), any<Any>(), any<Map<String, String>>()) } throws
            CancellationException("cancelled")
        val failedAudit = adapter().capture(order)
        assertFailsWith<CancellationException> { failedAudit.audit(failing, "adapter") }
        assertFailsWith<IllegalStateException> { failedAudit.complete(AuditCompletion.COMMITTED) }
        order.domainEvents() shouldHaveSize 1
    }

    @Test
    fun `부분 publish 재시도는 이미 전달한 이벤트를 중복 전달할 수 있다`() {
        val order = Order().apply { emit(); emit() }
        val first = order.domainEvents().first()
        val delivered = mutableListOf<ExposedEvent<Long>>()
        val registration = adapter().capture(order)
        registration.audit(javers(), "adapter")
        assertFailsWith<IllegalArgumentException> {
            registration.publish {
                if (it !== first) throw IllegalArgumentException("publisher")
                delivered += it
            }
        }
        assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
        order.domainEvents() shouldHaveSize 2
        val retry = adapter().capture(order)
        retry.audit(javers(), "adapter")
        retry.publish { delivered += it }
        retry.complete(AuditCompletion.COMMITTED)
        delivered.count { it === first } shouldBeEqualTo 2
    }

    @Test
    fun `캡처 후 추가 교체 순서 변경은 clear 전에 거부한다`() {
        listOf<(Order) -> Unit>(
            { it.emit() },
            { order -> val old = order.domainEvents(); order.clearDomainEvents(); old.reversed().forEach(order::add) },
            { order -> order.clearDomainEvents(); order.emit(); order.emit() },
        ).forEach { mutate ->
            val order = Order().apply { emit(); emit() }
            val registration = adapter().capture(order)
            registration.audit(javers(), "adapter")
            registration.publish {}
            mutate(order)
            val retained = order.domainEvents()
            assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
            order.domainEvents() shouldBeEqualTo retained
        }
    }

    @Test
    fun `빈 캡처 뒤 추가한 이벤트와 callback에서 추가한 이벤트를 보존한다`() {
        val empty = Order()
        val registration = adapter().capture(empty)
        empty.emit()
        assertFailsWith<IllegalStateException> { registration.audit(javers(), "adapter") }
        empty.domainEvents() shouldHaveSize 1
        val order = Order().apply { emit() }
        val publishing = adapter().capture(order)
        publishing.audit(javers(), "adapter")
        publishing.publish { order.emit() }
        assertFailsWith<IllegalStateException> { publishing.complete(AuditCompletion.COMMITTED) }
        order.domainEvents() shouldHaveSize 2
    }

    @Test
    fun `callback 재진입은 진행 중 단계를 재실행하지 않는다`() {
        val order = Order().apply { emit() }
        val registration = adapter().capture(order)
        registration.audit(javers(), "adapter")
        registration.publish {
            assertFailsWith<IllegalStateException> { registration.publish {} }
            assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
        }
        registration.complete(AuditCompletion.COMMITTED)
    }

    @Test
    fun `clear 실패와 계약 밖 부분 clear는 재시도하지 않고 캡처 증거를 남긴다`() {
        listOf(false, true).forEach { partial ->
            val order = Order().apply { emit() }
            val registration = adapter(clear = {
                if (partial) it.clearDomainEvents()
                throw IllegalStateException("clear")
            }).capture(order)
            registration.audit(javers(), "adapter")
            registration.publish {}
            assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
            assertFailsWith<IllegalStateException> { registration.complete(AuditCompletion.COMMITTED) }
            registration.events shouldHaveSize 1
            order.domainEvents().size shouldBeEqualTo if (partial) 0 else 1
        }
    }

    @Test
    fun `100개 이벤트도 mapper와 publisher를 한 번씩만 호출한다`() {
        val order = Order().apply { repeat(100) { emit() } }
        var mappingCount = 0
        var published = 0
        val registration = adapter(mapper = { mappingCount++; mapped(it) }).capture(order)
        val commit = registration.audit(javers(), "adapter")
        commit.properties["events.0.aggregateId"] shouldBeEqualTo "42"
        commit.properties["events.99.occurredOn"] shouldBeEqualTo "2026-09-08T00:00:00Z"
        commit.properties["events.0.domainEventType"] shouldBeEqualTo Placed::class.java.name
        commit.properties["events.99.event.tenant"] shouldBeEqualTo "blue"
        registration.publish { published++ }
        registration.complete(AuditCompletion.COMMITTED)
        mappingCount shouldBeEqualTo 100
        published shouldBeEqualTo 100
    }

    @Test
    fun `audit 이전 rollback과 unknown도 buffer를 유지하고 종료한다`() {
        listOf(AuditCompletion.ROLLED_BACK, AuditCompletion.UNKNOWN).forEach { completion ->
            val order = Order().apply { emit() }
            val registration = adapter(clear = { error("clear forbidden") }).capture(order)
            registration.complete(completion)
            order.domainEvents() shouldHaveSize 1
            assertFailsWith<IllegalStateException> { registration.audit(javers(), "adapter") }
        }
    }

    @Test
    fun `익명 이벤트 기본 type과 callback 취소 객체 및 발행 순서를 보존한다`() {
        val anonymous = object: DomainEvent {
            override val aggregateId = 1L
            override val occurredOn = Instant.EPOCH
        }
        anonymous.eventType shouldBeEqualTo anonymous.javaClass.name
        val order = Order().apply { emit(); emit() }
        val originals = order.domainEvents()
        val registration = adapter().capture(order)
        registration.audit(javers(), "adapter")
        val delivered = mutableListOf<ExposedEvent<Long>>()
        val cancellation = CancellationException("callback cancelled")
        val caught = assertFailsWith<CancellationException> {
            registration.publish { delivered += it; if (delivered.size == 2) throw cancellation }
        }
        (caught === cancellation) shouldBeEqualTo true
        delivered.indices.all { delivered[it] === originals[it] } shouldBeEqualTo true
        order.domainEvents() shouldHaveSize 2
    }

    class Order(@field:Id override val id: Long = 42L): AbstractAggregateRoot<Long>() {
        fun emit() = recordDomainEvent(Placed(id))
        fun add(event: ExposedEvent<Long>) = recordDomainEvent(event)
    }

    data class Placed(
        override val aggregateId: Long,
        override val occurredAt: Instant = Instant.parse("2026-09-08T00:00:00Z"),
    ): ExposedEvent<Long>

    companion object {
        private fun mapped(event: ExposedEvent<Long>): DomainEvent = object: DomainEvent {
            override val aggregateId = event.aggregateId
            override val occurredOn = event.occurredAt
            override val eventType = event.javaClass.name
            override val attributes = mapOf("tenant" to "blue")
        }
    }
}
