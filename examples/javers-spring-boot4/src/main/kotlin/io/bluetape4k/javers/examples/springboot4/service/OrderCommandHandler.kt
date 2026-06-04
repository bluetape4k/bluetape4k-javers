package io.bluetape4k.javers.examples.springboot4.service

import io.bluetape4k.javers.examples.springboot4.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.springboot4.domain.Order
import io.bluetape4k.javers.examples.springboot4.domain.OrderMarkedPaid
import io.bluetape4k.javers.examples.springboot4.domain.OrderPlaced
import io.bluetape4k.javers.examples.springboot4.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.springboot4.persistence.OrderRepository
import java.time.Clock

/**
 * Command handler for the order Spring Boot 4 example.
 *
 * ## Contract
 * Each handler method performs one aggregate state transition, persists the
 * aggregate through [OrderRepository], commits the state to JaVers, and
 * publishes the matching domain event.
 */
class OrderCommandHandler(
    private val repository: OrderRepository,
    private val clock: Clock,
) {

    fun handle(command: PlaceOrderCommand): Order {
        val now = clock.instant()
        val order = Order.place(
            id = command.orderId,
            customerId = command.customerId,
            items = command.items,
            now = now,
        )
        return repository.save(
            aggregate = order,
            author = command.author,
            event = OrderPlaced(
                aggregateId = order.id,
                occurredOn = now,
                customerId = order.customerId,
                totalAmount = order.totalAmount.toPlainString(),
            ),
        )
    }

    fun handle(command: MarkOrderPaidCommand): Order {
        val current = requireNotNull(repository.load(command.orderId)) {
            "Order not found: ${command.orderId.value}"
        }
        val now = clock.instant()
        val paid = current.markPaid(now)
        return repository.save(
            aggregate = paid,
            author = command.author,
            event = OrderMarkedPaid(
                aggregateId = paid.id,
                occurredOn = now,
            ),
        )
    }
}
