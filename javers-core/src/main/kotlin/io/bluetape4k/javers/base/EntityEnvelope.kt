package io.bluetape4k.javers.base

import java.io.Serializable

/**
 * entity state-change event를 전달하는 envelope object입니다.
 *
 * ## 동작 / 계약
 * - [entity] 기반 constructor는 기본적으로 SAVED event를 설정합니다.
 * - [entityId] + [entityType] constructor는 event를 DELETED로 설정합니다.
 * - 사용자 정의 metadata는 [addHeader] / [getHeader]로 관리합니다.
 * - headers는 envelope payload의 보조 metadata이므로 data class의 구조적 identity에 포함되지 않습니다.
 *   따라서 generated `copy`, `equals`, `hashCode`, `toString`은 headers를 무시하며 `copy` 결과는 빈 headers로 시작합니다.
 * - Java serialization은 headers를 포함한 전체 object state를 보존합니다.
 *
 * ```kotlin
 * val saved = EntityEnvelope(myEntity)
 * // saved.isSavedEntity == true
 *
 * val deleted = EntityEnvelope(entityId = 1L, entityType = User::class.java)
 * // deleted.isDeletedEntity == true
 * ```
 *
 * @property entity state가 변경된 entity입니다. SAVED event에서만 설정됩니다.
 * @property entityId 삭제된 entity의 ID입니다. DELETED event에서만 설정됩니다.
 * @property entityType entity의 Java class type입니다.
 * @property eventType event type입니다. 기본값은 [EntityEventType.SAVED]입니다.
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

    /** 구조적 identity와 별도로 보존되는 사용자 정의 header의 read-only view입니다. */
    val headers: Map<String, String> get() = _headers

    /**
     * 사용자 정의 header entry를 추가하거나 교체합니다.
     *
     * @param key header 이름입니다.
     * @param value header 값입니다.
     */
    fun addHeader(key: String, value: String) {
        _headers[key] = value
    }

    /**
     * 지정한 key의 header 값을 반환하며, 없으면 `null`을 반환합니다.
     *
     * @param key 조회할 header 이름입니다.
     */
    fun getHeader(key: String): String? = _headers[key]

    /** event type이 SAVED이면 `true`입니다. */
    val isSavedEntity: Boolean get() = eventType == EntityEventType.SAVED

    /** event type이 DELETED이면 `true`입니다. */
    val isDeletedEntity: Boolean get() = eventType == EntityEventType.DELETED
}
