package io.bluetape4k.javers.ddd

import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.QueryBuilder

/**
 * aggregate persistence와 JaVers auditing을 결합하는 base repository입니다.
 *
 * ## 계약
 * subclass는 [persist]와 [findById]에서 source-of-truth persistence를 소유합니다.
 * 이 base class는 [saveAuditBoundary] 안에서 저장된 aggregate를 JaVers에 commit하고,
 * history reconstruction을 위해 JaVers shadow를 load하며,
 * persistence와 JaVers commit이 성공한 뒤 domain event를 publish합니다.
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
     * [aggregate]를 저장하고 현재 state를 JaVers에 commit한 뒤 저장된 aggregate를 반환합니다.
     */
    fun save(aggregate: T, author: String): T = save(aggregate, author, emptyList())

    /**
     * [aggregate]를 저장하고 현재 state를 JaVers에 commit하며 [event]를 publish한 뒤 저장된 aggregate를 반환합니다.
     */
    fun save(aggregate: T, author: String, event: DomainEvent): T = save(aggregate, author, listOf(event))

    /**
     * [aggregate]를 저장하고 현재 state를 JaVers에 commit하며 [events]를 publish한 뒤 저장된 aggregate를 반환합니다.
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
     * 먼저 subclass persistence store에서 aggregate를 load하고, 없으면 최신 JaVers shadow로 fallback합니다.
     */
    fun load(id: ID): T? {
        return findById(id) ?: loadLatestShadow(id)
    }

    /**
     * aggregate id의 JaVers snapshot을 역시간순으로 load합니다.
     */
    fun loadHistory(id: ID): List<CdoSnapshot> {
        return javers.findSnapshots(QueryBuilder.byInstanceId(id, aggregateType).build())
    }

    /**
     * source-of-truth store에 aggregate를 persist합니다.
     */
    protected abstract fun persist(aggregate: T): T

    /**
     * source persistence와 JaVers audit commit을 하나의 save boundary 안에서 실행합니다.
     *
     * ## 계약
     * 기본 boundary는 shared transaction을 노출하지 않는 store를 위한 direct call입니다.
     * transaction-aware subclass는 이 method를 override하고 source state와 JaVers audit state가
     * 함께 commit 또는 rollback되도록 [block]을 source store transaction 안에서 실행해야 합니다.
     */
    protected open fun <R> saveAuditBoundary(block: () -> R): R = block()

    /**
     * source-of-truth store에서 aggregate를 찾습니다.
     */
    protected abstract fun findById(id: ID): T?

    private fun loadLatestShadow(id: ID): T? {
        val query = QueryBuilder.byInstanceId(id, aggregateType)
            .limit(1)
            .build()
        return javers.findShadows<T>(query).firstOrNull()?.get()
    }
}
