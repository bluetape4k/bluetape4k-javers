package io.bluetape4k.javers.persistence.exposed.hook

import org.javers.repository.jql.GlobalIdDTO
import org.javers.repository.jql.InstanceIdDTO
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.toEntity

/**
 * 하나의 Exposed DAO entity class를 하나의 detached JaVers audit type으로 mapping합니다.
 *
 * ## 계약
 * mapper는 `Created`와 `Updated` lifecycle event에서만 호출됩니다.
 * transaction-bound DAO entity 자체가 아니라 detached domain object 또는 DTO를 반환해야 합니다.
 * `Removed` event는 [toAuditId]와 [auditType]을 사용해 id 기반 JaVers terminal snapshot을 생성합니다.
 *
 * ```kotlin
 * val mapping = ExposedJaversEntityHookMapping.of(CustomerEntity) { entity ->
 *     AuditedCustomer(entity.id.value, entity.name)
 * }
 * ```
 */
class ExposedJaversEntityHookMapping<ID: Any, E: Entity<ID>, A: Any>(
    val entityClass: EntityClass<ID, E>,
    val auditType: Class<A>,
    val toAuditObject: (E) -> A,
    val toAuditId: (EntityID<ID>) -> Any = { it.value },
) {

    internal fun matches(change: EntityChange): Boolean {
        return change.entityClass.isAssignableTo(entityClass)
    }

    internal fun toAuditObject(change: EntityChange): A? {
        return change.toEntity(entityClass)?.let(toAuditObject)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun toGlobalId(change: EntityChange): GlobalIdDTO {
        val entityId = change.entityId as EntityID<ID>
        return InstanceIdDTO.instanceId(toAuditId(entityId), auditType)
    }

    companion object {
        /**
         * [A]에서 audit type을 추론해 mapping을 생성합니다.
         */
        inline fun <ID: Any, E: Entity<ID>, reified A: Any> of(
            entityClass: EntityClass<ID, E>,
            noinline toAuditId: (EntityID<ID>) -> Any = { it.value },
            noinline toAuditObject: (E) -> A,
        ): ExposedJaversEntityHookMapping<ID, E, A> {
            return ExposedJaversEntityHookMapping(
                entityClass = entityClass,
                auditType = A::class.java,
                toAuditObject = toAuditObject,
                toAuditId = toAuditId,
            )
        }
    }
}
