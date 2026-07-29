package io.bluetape4k.javers.examples.springboot4.domain

import io.bluetape4k.javers.ddd.DomainEvent
import java.io.Serializable
import java.time.Instant

/**
 * 주문이 생성될 때 발행되는 event입니다.
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
 * 주문이 결제 완료로 표시될 때 발행되는 event입니다.
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
