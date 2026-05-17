package io.bluetape4k.javers

import org.javers.core.graph.Cdo
import kotlin.jvm.optionals.getOrNull

/**
 * Returns the domain object wrapped by this [Cdo], or null if none is present.
 *
 * ```kotlin
 * val wrapped = cdo.getWrappedOrNull()
 * // null or the underlying domain object
 * ```
 */
fun Cdo.getWrappedOrNull(): Any? = this.wrappedCdo.getOrNull()
