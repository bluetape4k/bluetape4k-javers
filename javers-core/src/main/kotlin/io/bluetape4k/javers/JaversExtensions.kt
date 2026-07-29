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
 * reified type에 대한 [EntityType] mapping 정보를 조회합니다.
 *
 * ```kotlin
 * val entityType = javers.getEntityTypeMapping<Person>()
 * // entityType.baseJavaType == Person::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getEntityTypeMapping(): EntityType =
    this.getTypeMapping(T::class.java)

/**
 * reified type에 대한 [ValueObjectType] mapping 정보를 조회합니다.
 *
 * ```kotlin
 * val voType = javers.getValueObjectTypeMapping<Address>()
 * // voType.baseJavaType == Address::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getValueObjectTypeMapping(): ValueObjectType =
    this.getTypeMapping(T::class.java)

/**
 * entity instance에서 [InstanceId]를 생성합니다.
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
 * entity local id 값에서 [InstanceId]를 생성합니다.
 *
 * ```kotlin
 * val id = javers.createEntityInstanceIdByEntityId<Person>("Bob")
 * // id.value() == "Person/Bob"
 * ```
 */
inline fun <reified T: Any> Javers.createEntityInstanceIdByEntityId(localId: Any): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstanceId(localId)

/**
 * 두 collection을 비교하고 그 결과 [Diff]를 반환합니다.
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
 * 지정한 entity의 최신 [CdoSnapshot]을 반환하며, 존재하지 않으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull(1, SnapshotEntity::class)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
fun Javers.latestSnapshotOrNull(localId: Any, entityClass: KClass<*>): CdoSnapshot? =
    getLatestSnapshot(localId, entityClass.java).getOrNull()

/**
 * reified entity type의 최신 [CdoSnapshot]을 반환하며, 존재하지 않으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull<SnapshotEntity>(1)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
inline fun <reified T: Any> Javers.latestSnapshotOrNull(localId: Any): CdoSnapshot? =
    getLatestSnapshot(localId, T::class.java).getOrNull()

/**
 * [CdoSnapshot]을 복원된 entity를 감싸는 [Shadow]로 변환합니다.
 *
 * ## 계약
 * - [ShadowFactory]를 통해 snapshot state를 entity object로 복원합니다.
 * - 반환된 [Shadow]는 `get()`으로 복원된 entity를 노출합니다.
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
 * 이 [Javers] instance와 연결된 [ShadowFactory]를 반환합니다.
 */
val Javers.shadowFactory: ShadowFactory
    get() = ShadowProvider.getShadowFactory(this)

/**
 * JQL로 shadow를 조회하고 [Sequence]로 반환합니다.
 *
 * ```kotlin
 * val query = queryByInstanceId<SnapshotEntity>(1)
 * val shadows = javers.findShadowsAndSequence<SnapshotEntity>(query)
 * // shadows.toList().size >= 1
 * ```
 */
fun <T: Any> Javers.findShadowsAndSequence(jql: JqlQuery): Sequence<Shadow<T>> =
    findShadowsAndStream<T>(jql).asSequence()
