package io.bluetape4k.javers.examples.contract

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.ddd.AggregateRepository
import io.bluetape4k.javers.ddd.AggregateRoot
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.mockk.every
import io.mockk.mockk
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** 세 독립 예제의 실제 주문 모델과 저장소에 같은 행동 계약을 적용합니다. */
abstract class OrderBehaviorContract<O: AggregateRoot<ID>, ID: Any> {
    protected abstract val orderType: Class<O>
    protected abstract val ordersTable: Table
    protected abstract fun order(quantity: Int = 2, empty: Boolean = false): O
    protected abstract fun paid(order: O): O
    protected abstract fun total(order: O): BigDecimal
    protected abstract fun status(order: O): String
    protected abstract fun repository(database: Database, javers: Javers): AggregateRepository<O, ID>

    private fun database(): Database = Database.connect(
        "jdbc:h2:mem:contract-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "org.h2.Driver",
    ).also { database ->
        ExposedCdoSnapshotRepository(database).ensureSchema()
        transaction(database) { SchemaUtils.create(ordersTable) }
    }

    @Test
    fun `주문 검증과 상태 전이를 동일하게 제한한다`() {
        assertFailsWith<IllegalArgumentException> { order(empty = true) }
        assertFailsWith<IllegalArgumentException> { order(quantity = 0) }
        assertFailsWith<IllegalArgumentException> { order(quantity = -1) }
        val placed = order()
        total(placed) shouldBeEqualTo BigDecimal("25.00")
        status(placed) shouldBeEqualTo "PLACED"
        status(paid(placed)) shouldBeEqualTo "PAID"
        assertFailsWith<IllegalArgumentException> { paid(paid(placed)) }
    }

    @Test
    fun `저장된 주문과 감사 이력은 같은 상태 전이를 보존한다`() {
        val database = database()
        val javers = JaversBuilder.javers()
            .registerJaversRepository(ExposedCdoSnapshotRepository(database))
            .registerEntity(orderType).build()
        val repository = repository(database, javers)
        val placed = order()
        repository.save(placed, "contract")
        repository.save(paid(placed), "contract")

        status(repository.load(placed.id).shouldNotBeNull()) shouldBeEqualTo "PAID"
        val history = repository.loadHistory(placed.id)
        history shouldHaveSize 2
        history.map { it.getPropertyValue("status").toString() } shouldBeEqualTo listOf("PAID", "PLACED")
    }

    @Test
    fun `감사 저장 실패는 주문 row도 rollback한다`() {
        val database = database()
        val failing = mockk<Javers>()
        every { failing.commit(any<String>(), any<Any>(), any<Map<String, String>>()) } throws
            IllegalStateException("audit failed")
        val repository = repository(database, failing)

        assertFailsWith<IllegalStateException> { repository.save(order(), "contract") }
        transaction(database) { ordersTable.selectAll().count() } shouldBeEqualTo 0L
    }
}
