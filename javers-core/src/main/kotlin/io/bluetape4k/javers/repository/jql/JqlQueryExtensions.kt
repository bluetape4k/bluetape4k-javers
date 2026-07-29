package io.bluetape4k.javers.repository.jql

import org.javers.core.Changes
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * 이 [JqlQuery]로 shadow를 조회합니다.
 *
 * ```kotlin
 * val query = queryByInstanceId<Person>("bob")
 * val shadows = query.findShadows<Person>(javers)
 * ```
 */
inline fun <reified T: Any> JqlQuery.findShadows(javers: Javers): MutableList<Shadow<T>> =
    javers.findShadows(this)

/**
 * 이 [JqlQuery]로 shadow를 조회하고 [Stream]으로 반환합니다.
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndStream(javers: Javers): Stream<Shadow<T>> =
    javers.findShadowsAndStream(this)

/**
 * 이 [JqlQuery]로 shadow를 조회하고 [Sequence]로 반환합니다.
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndSequence(javers: Javers): Sequence<Shadow<T>> =
    javers.findShadowsAndStream<T>(this).asSequence()

/**
 * 이 [JqlQuery]로 snapshot을 조회합니다.
 */
fun JqlQuery.findSnapshots(javers: Javers): MutableList<CdoSnapshot> =
    javers.findSnapshots(this)

/**
 * 이 [JqlQuery]로 change를 조회합니다.
 */
fun JqlQuery.findChanges(javers: Javers): Changes =
    javers.findChanges(this)
