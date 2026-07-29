package io.bluetape4k.javers

import org.javers.core.graph.Cdo
import kotlin.jvm.optionals.getOrNull

/**
 * 이 [Cdo]가 감싸는 domain object를 반환하며, 값이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val wrapped = cdo.getWrappedOrNull()
 * // null 또는 내부 domain object
 * ```
 */
fun Cdo.getWrappedOrNull(): Any? = this.wrappedCdo.getOrNull()
