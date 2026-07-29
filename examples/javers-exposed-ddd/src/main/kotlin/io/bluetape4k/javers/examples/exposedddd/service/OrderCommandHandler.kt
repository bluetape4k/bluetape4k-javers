package io.bluetape4k.javers.examples.exposedddd.service

import io.bluetape4k.javers.examples.exposedddd.domain.MarkOrderPaidCommand
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderMarkedPaid
import io.bluetape4k.javers.examples.exposedddd.domain.OrderPlaced
import io.bluetape4k.javers.examples.exposedddd.domain.PlaceOrderCommand
import io.bluetape4k.javers.examples.exposedddd.persistence.OrderRepository
import java.time.Clock

/**
 * 주문 command-side 예제의 command handler입니다.
 *
 * ## 계약
 * 각 handler method는 aggregate 상태 전이를 하나 수행하고, [OrderRepository]를
 * 통해 aggregate를 저장한 뒤, 상태를 JaVers에 commit하고 대응하는 domain event를
 * 발행합니다.
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
