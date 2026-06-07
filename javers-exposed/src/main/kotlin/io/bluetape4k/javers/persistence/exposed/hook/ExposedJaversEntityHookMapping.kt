package io.bluetape4k.javers.persistence.exposed.hook

import org.javers.repository.jql.GlobalIdDTO
import org.javers.repository.jql.InstanceIdDTO
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.toEntity

/**
 * Maps one Exposed DAO entity class to one detached JaVers audit type.
 *
 * ## Contract
 * The mapper is invoked only for `Created` and `Updated` lifecycle events. It
 * should return a detached domain object or DTO, not the transaction-bound DAO
 * entity itself. `Removed` events use [toAuditId] and [auditType] to create a
 * JaVers terminal snapshot by id.
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
         * Creates a mapping with an audit type inferred from [A].
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
