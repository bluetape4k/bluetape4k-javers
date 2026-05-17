package io.bluetape4k.javers.base

import java.io.Serializable

/**
 * Envelope object used to carry entity state-change events.
 *
 * ## Behavior / Contract
 * - The [entity]-based constructor defaults to a SAVED event.
 * - The [entityId] + [entityType] constructor sets the event to DELETED.
 * - Custom metadata is managed via [addHeader] / [getHeader].
 *
 * ```kotlin
 * val saved = EntityEnvelope(myEntity)
 * // saved.isSavedEntity == true
 *
 * val deleted = EntityEnvelope(entityId = 1L, entityType = User::class.java)
 * // deleted.isDeletedEntity == true
 * ```
 *
 * @property entity the entity whose state changed (set only for SAVED events)
 * @property entityId the ID of the deleted entity (set only for DELETED events)
 * @property entityType the Java class type of the entity
 * @property eventType the event type (default: [EntityEventType.SAVED])
 */
data class EntityEnvelope(
    val entity: Any? = null,
    val entityId: Any? = null,
    val entityType: Class<*>,
    val eventType: EntityEventType = EntityEventType.SAVED,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    constructor(entity: Any): this(entity = entity, entityType = entity.javaClass)
    constructor(entityId: Any, entityType: Class<*>): this(null, entityId, entityType, EntityEventType.DELETED)

    private val _headers: MutableMap<String, String> = hashMapOf()

    /** Read-only view of the custom headers attached to this envelope. */
    val headers: Map<String, String> get() = _headers

    /**
     * Adds or replaces a custom header entry.
     *
     * @param key the header name
     * @param value the header value
     */
    fun addHeader(key: String, value: String) {
        _headers[key] = value
    }

    /**
     * Returns the value of the header with the given key, or null if absent.
     *
     * @param key the header name
     */
    fun getHeader(key: String): String? = _headers[key]

    /** True when the event type is SAVED. */
    val isSavedEntity: Boolean get() = eventType == EntityEventType.SAVED

    /** True when the event type is DELETED. */
    val isDeletedEntity: Boolean get() = eventType == EntityEventType.DELETED
}
