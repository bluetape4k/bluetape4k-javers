package io.bluetape4k.javers.repository.jql

import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import kotlin.reflect.KClass

/**
 * Creates a [JqlQuery] targeting all domain objects.
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
 * Creates a [JqlQuery] targeting entities of the specified type.
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
 * Creates a [JqlQuery] targeting a specific entity instance.
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
 * Creates a [JqlQuery] from an entity local id.
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
 * Creates a [JqlQuery] for value objects of the specified type at [path].
 */
inline fun <reified T: Any> queryByValueObject(
    path: String,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObject(T::class.java, path).apply(builder).build()
}

/**
 * Creates a [JqlQuery] for a value object by owner local id and [path].
 */
inline fun <reified T: Any> queryByValueObjectId(
    ownerLocalId: Any,
    path: String,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObjectId(ownerLocalId, T::class.java, path).apply(builder).build()
}

/**
 * Creates a [JqlQuery] targeting the specified class.
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
 * Creates a [JqlQuery] targeting multiple Java classes from a [Collection].
 */
@JvmName("queryByClassesCollection")
inline fun queryByClasses(
    classes: Collection<Class<*>>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes.toTypedArray()).apply(builder).build()
}

/**
 * Creates a [JqlQuery] targeting multiple Java classes from vararg input.
 */
@JvmName("queryByClassesArray")
inline fun queryByClasses(
    vararg classes: Class<*>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes).apply(builder).build()
}

/**
 * Creates a [JqlQuery] targeting multiple Kotlin classes from a [Collection].
 */
inline fun queryByClasses(
    kclasses: Collection<KClass<*>>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}

/**
 * Creates a [JqlQuery] targeting multiple Kotlin classes from vararg input.
 */
inline fun queryByClasses(
    vararg kclasses: KClass<*>,
    builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}
