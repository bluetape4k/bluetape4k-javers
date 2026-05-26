package io.bluetape4k.javers.examples.exposedddd.persistence

import com.google.gson.Gson
import io.bluetape4k.javers.ddd.AggregateRepository
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.bluetape4k.javers.ddd.NoopDomainEventPublisher
import io.bluetape4k.javers.examples.exposedddd.domain.CustomerId
import io.bluetape4k.javers.examples.exposedddd.domain.Order
import io.bluetape4k.javers.examples.exposedddd.domain.OrderId
import io.bluetape4k.javers.examples.exposedddd.domain.OrderItem
import io.bluetape4k.javers.examples.exposedddd.domain.OrderStatus
import org.javers.core.Javers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Exposed-backed source-of-truth repository for the order command side.
 *
 * ## Contract
 * This repository persists order state in [OrdersTable], then delegates JaVers
 * commits and event publication to [AggregateRepository].
 */
class OrderRepository(
    private val database: Database,
    javers: Javers,
    eventPublisher: DomainEventPublisher = NoopDomainEventPublisher,
): AggregateRepository<Order, OrderId>(Order::class.java, javers, eventPublisher) {

    private val gson = Gson()

    override fun persist(aggregate: Order): Order {
        transaction(database) {
            val exists = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq aggregate.id.value }
                .limit(1)
                .empty()
                .not()

            if (exists) {
                OrdersTable.update({ OrdersTable.id eq aggregate.id.value }) {
                    it[customerId] = aggregate.customerId.value
                    it[status] = aggregate.status.name
                    it[itemsJson] = gson.toJson(aggregate.items)
                    it[createdAt] = aggregate.createdAt
                    it[updatedAt] = aggregate.updatedAt
                }
            } else {
                OrdersTable.insert {
                    it[id] = aggregate.id.value
                    it[customerId] = aggregate.customerId.value
                    it[status] = aggregate.status.name
                    it[itemsJson] = gson.toJson(aggregate.items)
                    it[createdAt] = aggregate.createdAt
                    it[updatedAt] = aggregate.updatedAt
                }
            }
        }
        return aggregate
    }

    override fun findById(id: OrderId): Order? = transaction(database) {
        OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id.value }
            .limit(1)
            .singleOrNull()
            ?.toOrder()
    }

    private fun ResultRow.toOrder(): Order {
        val items = gson.fromJson(this[OrdersTable.itemsJson], Array<OrderItem>::class.java).toList()
        return Order(
            id = OrderId(this[OrdersTable.id]),
            customerId = CustomerId(this[OrdersTable.customerId]),
            items = items,
            status = OrderStatus.valueOf(this[OrdersTable.status]),
            createdAt = this[OrdersTable.createdAt],
            updatedAt = this[OrdersTable.updatedAt],
        )
    }
}
