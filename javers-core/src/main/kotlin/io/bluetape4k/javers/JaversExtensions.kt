package io.bluetape4k.javers

import org.javers.core.Javers
import org.javers.core.diff.Diff
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ValueObjectType
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import org.javers.shadow.ShadowFactory
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.streams.asSequence

/**
 * Looks up [EntityType] mapping information for the reified type.
 *
 * ```kotlin
 * val entityType = javers.getEntityTypeMapping<Person>()
 * // entityType.baseJavaType == Person::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getEntityTypeMapping(): EntityType =
    this.getTypeMapping(T::class.java)

/**
 * Looks up [ValueObjectType] mapping information for the reified type.
 *
 * ```kotlin
 * val voType = javers.getValueObjectTypeMapping<Address>()
 * // voType.baseJavaType == Address::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getValueObjectTypeMapping(): ValueObjectType =
    this.getTypeMapping(T::class.java)

/**
 * Creates an [InstanceId] from an entity instance.
 *
 * ```kotlin
 * val bob = Person("Bob", "Dev")
 * val id = javers.createEntityInstanceId(bob)
 * // id.value() == "Person/Bob"
 * ```
 */
inline fun <reified T: Any> Javers.createEntityInstanceId(entity: T): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstance(entity)

/**
 * Creates an [InstanceId] from an entity local id value.
 *
 * ```kotlin
 * val id = javers.createEntityInstanceIdByEntityId<Person>("Bob")
 * // id.value() == "Person/Bob"
 * ```
 */
inline fun <reified T: Any> Javers.createEntityInstanceIdByEntityId(localId: Any): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstanceId(localId)

/**
 * Compares two collections and returns their [Diff].
 *
 * ```kotlin
 * val oldList = listOf(Person("Tommy", "Tommy Smart"))
 * val newList = listOf(Person("Tommy", "Tommy C. Smart"))
 * val diff = javers.compareCollections(oldList, newList)
 * // diff.changes.size == 1
 * ```
 */
inline fun <reified T: Any> Javers.compareCollections(oldVersion: Collection<T>, newVersion: Collection<T>): Diff =
    this.compareCollections(oldVersion, newVersion, T::class.java)

/**
 * Returns the latest [CdoSnapshot] for the specified entity, or `null` when none exists.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull(1, SnapshotEntity::class)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
fun Javers.latestSnapshotOrNull(localId: Any, entityClass: KClass<*>): CdoSnapshot? =
    getLatestSnapshot(localId, entityClass.java).getOrNull()

/**
 * Returns the latest [CdoSnapshot] for the reified entity type, or `null` when none exists.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull<SnapshotEntity>(1)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
inline fun <reified T: Any> Javers.latestSnapshotOrNull(localId: Any): CdoSnapshot? =
    getLatestSnapshot(localId, T::class.java).getOrNull()

/**
 * Converts a [CdoSnapshot] into a [Shadow] wrapping the reconstructed entity.
 *
 * ## Contract
 * - Restores the snapshot state into an entity object through [ShadowFactory].
 * - The returned [Shadow] exposes the restored entity through `get()`.
 *
 * ```kotlin
 * val snapshot = javers.commit("a", entity).snapshots.first()
 * val shadow: Shadow<SnapshotEntity> = javers.getShadow(snapshot)
 * // shadow.get() == entity
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T> Javers.getShadow(snapshot: CdoSnapshot): Shadow<T> {
    return shadowFactory.createShadow(snapshot, snapshot.commitMetadata, null) as Shadow<T>
}

/**
 * Returns the [ShadowFactory] associated with this [Javers] instance.
 */
val Javers.shadowFactory: ShadowFactory
    get() = ShadowProvider.getShadowFactory(this)

/**
 * Queries shadows with JQL and returns them as a [Sequence].
 *
 * ```kotlin
 * val query = queryByInstanceId<SnapshotEntity>(1)
 * val shadows = javers.findShadowsAndSequence<SnapshotEntity>(query)
 * // shadows.toList().size >= 1
 * ```
 */
fun <T: Any> Javers.findShadowsAndSequence(jql: JqlQuery): Sequence<Shadow<T>> =
    findShadowsAndStream<T>(jql).asSequence()
