package io.bluetape4k.javers.examples.exposedddd.domain

import io.bluetape4k.javers.ddd.AggregateRoot
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requirePositiveNumber
import org.javers.core.metamodel.annotation.Id
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * command-side JaVers + Exposed 예제에서 사용하는 주문 aggregate입니다.
 *
 * ## 계약
 * 이 aggregate는 Exposed에 source of truth로 저장되고, command-side 상태 전이가
 * 끝날 때마다 JaVers에 commit됩니다. [id] 속성에는 JaVers [Id]가 지정되어
 * JaVers가 이 클래스를 entity로 매핑합니다.
 */
@ConsistentCopyVisibility
data class Order private constructor(
    @Id
    override val id: OrderId,
    val customerId: CustomerId,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
): AggregateRoot<OrderId>, Serializable {

    val totalAmount: BigDecimal
        get() = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.lineTotal }

    fun markPaid(now: Instant): Order {
        require(status == OrderStatus.PLACED) { "Only placed orders can be marked paid" }
        return copy(status = OrderStatus.PAID, updatedAt = now)
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        operator fun invoke(
            id: OrderId,
            customerId: CustomerId,
            items: List<OrderItem>,
            status: OrderStatus,
            createdAt: Instant,
            updatedAt: Instant,
        ): Order {
            validateItems(items)
            return Order(
                id = id,
                customerId = customerId,
                items = items,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

        fun place(
            id: OrderId,
            customerId: CustomerId,
            items: List<OrderItem>,
            now: Instant,
        ): Order {
            return invoke(
                id = id,
                customerId = customerId,
                items = items,
                status = OrderStatus.PLACED,
                createdAt = now,
                updatedAt = now,
            )
        }

        private fun validateItems(items: List<OrderItem>) {
            items.requireNotEmpty("items")
            items.forEach { item ->
                item.quantity.requirePositiveNumber("item.quantity")
                item.unitPrice.requirePositiveNumber("item.unitPrice")
            }
        }
    }
}
