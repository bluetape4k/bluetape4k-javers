package io.bluetape4k.javers.repository.jql

import org.javers.core.Changes
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * Finds shadows from this [JqlQuery].
 *
 * ```kotlin
 * val query = queryByInstanceId<Person>("bob")
 * val shadows = query.findShadows<Person>(javers)
 * ```
 */
inline fun <reified T: Any> JqlQuery.findShadows(javers: Javers): MutableList<Shadow<T>> =
    javers.findShadows(this)

/**
 * Finds shadows from this [JqlQuery] as a [Stream].
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndStream(javers: Javers): Stream<Shadow<T>> =
    javers.findShadowsAndStream(this)

/**
 * Finds shadows from this [JqlQuery] as a [Sequence].
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndSequence(javers: Javers): Sequence<Shadow<T>> =
    javers.findShadowsAndStream<T>(this).asSequence()

/**
 * Finds snapshots from this [JqlQuery].
 */
fun JqlQuery.findSnapshots(javers: Javers): MutableList<CdoSnapshot> =
    javers.findSnapshots(this)

/**
 * Finds changes from this [JqlQuery].
 */
fun JqlQuery.findChanges(javers: Javers): Changes =
    javers.findChanges(this)
