package io.bluetape4k.javers.repository.jql

import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import kotlin.reflect.KClass

/**
 * 모든 domain object를 대상으로 하는 [JqlQuery]를 생성합니다.
 *
 * ```kotlin
 * val query = queryAnyDomainObject { limit(10) }
 * val snapshots = javers.findSnapshots(query)
 * ```
 */
inline fun queryAnyDomainObject(
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.anyDomainObject().apply(builder).build()
}

/**
 * 지정한 type의 entity를 대상으로 하는 [JqlQuery]를 생성합니다.
 *
 * ```kotlin
 * val query = query<Person> { limit(5) }
 * val changes = javers.findChanges(query)
 * ```
 */
inline fun <reified T: Any> query(
    builder: QueryBuilder.() -> Unit,
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

/**
 * 특정 entity instance를 대상으로 하는 [JqlQuery]를 생성합니다.
 *
 * ```kotlin
 * val query = queryByInstance(person) { limit(10) }
 * val snapshots = javers.findSnapshots(query)
 * ```
 */
inline fun <reified T: Any> queryByInstance(
    instance: T,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstance(instance).apply(builder).build()
}

/**
 * entity local id에서 [JqlQuery]를 생성합니다.
 *
 * ```kotlin
 * val query = queryByInstanceId<Person>("bob")
 * val shadows = javers.findShadows<Person>(query)
 * // shadows.size >= 1
 * ```
 */
inline fun <reified T: Any> queryByInstanceId(
    localId: Any,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstanceId(localId, T::class.java).apply(builder).build()
}

/**
 * [path]에 있는 지정 type의 value object를 대상으로 하는 [JqlQuery]를 생성합니다.
 */
inline fun <reified T: Any> queryByValueObject(
    path: String,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObject(T::class.java, path).apply(builder).build()
}

/**
 * owner local id와 [path]로 value object 대상 [JqlQuery]를 생성합니다.
 */
inline fun <reified T: Any> queryByValueObjectId(
    ownerLocalId: Any,
    path: String,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObjectId(ownerLocalId, T::class.java, path).apply(builder).build()
}

/**
 * 지정한 class를 대상으로 하는 [JqlQuery]를 생성합니다.
 *
 * ```kotlin
 * val query = queryByClass<Person> { withNewObjectChanges() }
 * val changes = javers.findChanges(query)
 * ```
 */
inline fun <reified T: Any> queryByClass(
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

/**
 * [Collection]의 여러 Java class를 대상으로 하는 [JqlQuery]를 생성합니다.
 */
@JvmName("queryByClassesCollection")
inline fun queryByClasses(
    classes: Collection<Class<*>>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes.toTypedArray()).apply(builder).build()
}

/**
 * vararg input의 여러 Java class를 대상으로 하는 [JqlQuery]를 생성합니다.
 */
@JvmName("queryByClassesArray")
inline fun queryByClasses(
    vararg classes: Class<*>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes).apply(builder).build()
}

/**
 * [Collection]의 여러 Kotlin class를 대상으로 하는 [JqlQuery]를 생성합니다.
 */
inline fun queryByClasses(
    kclasses: Collection<KClass<*>>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}

/**
 * vararg input의 여러 Kotlin class를 대상으로 하는 [JqlQuery]를 생성합니다.
 */
inline fun queryByClasses(
    vararg kclasses: KClass<*>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}
