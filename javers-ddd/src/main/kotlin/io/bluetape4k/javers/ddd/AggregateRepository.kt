package io.bluetape4k.javers.ddd

import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.QueryBuilder

/**
 * Base repository that couples aggregate persistence with JaVers auditing.
 *
 * ## Contract
 * Subclasses own the source-of-truth persistence in [persist] and [findById].
 * This base class commits saved aggregates to JaVers inside [saveAuditBoundary],
 * loads JaVers shadows for history reconstruction, and publishes domain events
 * after persistence and JaVers commit succeed.
 *
 * ```kotlin
 * class OrderRepository(javers: Javers) :
 *     AggregateRepository<Order, Long>(Order::class.java, javers) {
 *     override fun persist(aggregate: Order): Order = aggregate
 *     override fun findById(id: Long): Order? = null
 * }
 * ```
 */
abstract class AggregateRepository<T: AggregateRoot<ID>, ID: Any>(
    private val aggregateType: Class<T>,
    private val javers: Javers,
    private val eventPublisher: DomainEventPublisher = NoopDomainEventPublisher,
) {

    /**
     * Saves [aggregate], commits its current state to JaVers, and returns the
     * saved aggregate.
     */
    fun save(aggregate: T, author: String): T = save(aggregate, author, emptyList())

    /**
     * Saves [aggregate], commits its current state to JaVers, publishes [event],
     * and returns the saved aggregate.
     */
    fun save(aggregate: T, author: String, event: DomainEvent): T = save(aggregate, author, listOf(event))

    /**
     * Saves [aggregate], commits its current state to JaVers, publishes [events],
     * and returns the saved aggregate.
     */
    fun save(aggregate: T, author: String, events: Collection<DomainEvent>): T {
        val saved = saveAuditBoundary {
            val persisted = persist(aggregate)
            javers.commit(author, persisted, events.toJaversProperties())
            persisted
        }
        eventPublisher.publishAll(events)
        return saved
    }

    /**
     * Loads the aggregate from the subclass persistence store first, then falls
     * back to the latest JaVers shadow.
     */
    fun load(id: ID): T? {
        return findById(id) ?: loadLatestShadow(id)
    }

    /**
     * Loads JaVers snapshots for the aggregate id in reverse chronological order.
     */
    fun loadHistory(id: ID): List<CdoSnapshot> {
        return javers.findSnapshots(QueryBuilder.byInstanceId(id, aggregateType).build())
    }

    /**
     * Persists the aggregate in the source-of-truth store.
     */
    protected abstract fun persist(aggregate: T): T

    /**
     * Runs source persistence and JaVers audit commit in one save boundary.
     *
     * ## Contract
     * The default boundary is a direct call for stores that do not expose a
     * shared transaction. Transaction-aware subclasses should override this and
     * execute [block] inside their source store transaction so source state and
     * JaVers audit state commit or roll back together.
     */
    protected open fun <R> saveAuditBoundary(block: () -> R): R = block()

    /**
     * Finds the aggregate in the source-of-truth store.
     */
    protected abstract fun findById(id: ID): T?

    private fun loadLatestShadow(id: ID): T? {
        val query = QueryBuilder.byInstanceId(id, aggregateType)
            .limit(1)
            .build()
        return javers.findShadows<T>(query).firstOrNull()?.get()
    }
}
