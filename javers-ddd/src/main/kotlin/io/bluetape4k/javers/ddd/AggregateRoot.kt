package io.bluetape4k.javers.ddd

/**
 * JaVers로 audit되는 DDD aggregate root의 marker contract입니다.
 *
 * ## 계약
 * [id] 값은 aggregate lifecycle 전체에서 stable해야 합니다.
 * type이 명시적 id property로 등록되어 있지 않다면 aggregate 구현은 override property에
 * JaVers `@Id`를 annotate해야 합니다.
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
     * aggregate lifecycle 동안 안정적으로 유지되는 identifier입니다.
     */
    val id: ID
}
