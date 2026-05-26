package io.bluetape4k.javers.ddd

/**
 * Marker contract for a DDD aggregate root audited through JaVers.
 *
 * ## Contract
 * The [id] value must be stable across the aggregate lifecycle. Aggregate
 * implementations should annotate the overriding property with JaVers `@Id`
 * unless the type is registered with an explicit id property.
 *
 * ```kotlin
 * data class Order(
 *     @Id
 *     override val id: Long,
 *     val status: String,
 * ) : AggregateRoot<Long>
 * ```
 */
interface AggregateRoot<ID: Any> {

    /**
     * Stable aggregate identifier.
     */
    val id: ID
}
