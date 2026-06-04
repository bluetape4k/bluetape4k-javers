package io.bluetape4k.javers.examples.ktor.domain

import io.bluetape4k.javers.ddd.DomainEvent
import java.io.Serializable
import java.time.Instant

/**
 * Event emitted when an order is placed.
 */
data class OrderPlaced(
    override val aggregateId: OrderId,
    override val occurredOn: Instant,
    val customerId: CustomerId,
    val totalAmount: String,
): DomainEvent, Serializable {
    override val attributes: Map<String, String> = mapOf(
        "customerId" to customerId.value,
        "totalAmount" to totalAmount,
    )

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Event emitted when an order is marked as paid.
 */
data class OrderMarkedPaid(
    override val aggregateId: OrderId,
    override val occurredOn: Instant,
): DomainEvent, Serializable {
    override val attributes: Map<String, String> = mapOf("status" to OrderStatus.PAID.name)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
