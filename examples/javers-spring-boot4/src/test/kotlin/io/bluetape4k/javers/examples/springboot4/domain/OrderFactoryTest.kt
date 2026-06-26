package io.bluetape4k.javers.examples.springboot4.domain

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class OrderFactoryTest {

    private val now: Instant = Instant.parse("2026-06-26T00:00:00Z")

    @Test
    fun `factory rejects empty items`() {
        assertFailsWith<IllegalArgumentException> {
            Order.place(
                id = OrderId("order-1"),
                customerId = CustomerId("customer-1"),
                items = emptyList(),
                now = now,
            )
        }
    }

    @Test
    fun `factory rejects non positive item fields`() {
        assertFailsWith<IllegalArgumentException> {
            Order.place(
                id = OrderId("order-2"),
                customerId = CustomerId("customer-1"),
                items = listOf(OrderItem("sku-1", quantity = 0, unitPrice = BigDecimal("12.50"))),
                now = now,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            Order.place(
                id = OrderId("order-3"),
                customerId = CustomerId("customer-1"),
                items = listOf(OrderItem("sku-1", quantity = 1, unitPrice = BigDecimal.ZERO)),
                now = now,
            )
        }
    }
}
