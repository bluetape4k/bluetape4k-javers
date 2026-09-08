package io.bluetape4k.javers.examples.ktor

import io.bluetape4k.javers.examples.contract.OrderBehaviorContract
import io.bluetape4k.javers.examples.ktor.domain.CustomerId
import io.bluetape4k.javers.examples.ktor.domain.Order
import io.bluetape4k.javers.examples.ktor.domain.OrderId
import io.bluetape4k.javers.examples.ktor.domain.OrderItem
import io.bluetape4k.javers.examples.ktor.persistence.OrderRepository
import io.bluetape4k.javers.examples.ktor.persistence.OrdersTable
import org.javers.core.Javers
import org.jetbrains.exposed.v1.jdbc.Database
import java.math.BigDecimal
import java.time.Instant

class SharedOrderContractTest: OrderBehaviorContract<Order, OrderId>() {
    override val orderType = Order::class.java
    override val ordersTable = OrdersTable
    private val time = Instant.parse("2026-09-08T00:00:00Z")

    override fun order(quantity: Int, empty: Boolean): Order = Order.place(
        id = OrderId("contract-order"), customerId = CustomerId("contract-customer"),
        items = if (empty) emptyList() else listOf(OrderItem("sku", quantity, BigDecimal("12.50"))),
        now = time,
    )
    override fun paid(order: Order): Order = order.markPaid(time.plusSeconds(1))
    override fun total(order: Order): BigDecimal = order.totalAmount
    override fun status(order: Order): String = order.status.name
    override fun repository(database: Database, javers: Javers) = OrderRepository(database, javers)
}
