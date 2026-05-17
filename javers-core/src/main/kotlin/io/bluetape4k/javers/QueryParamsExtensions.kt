package io.bluetape4k.javers

import org.javers.repository.api.QueryParams
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * Returns true if [date] falls within the from–to range of this [QueryParams].
 *
 * ## Behavior / Contract
 * - Returns false if `from` is set and is after [date].
 * - Returns false if `to` is set and is before [date].
 * - A boundary that is not set is treated as unbounded.
 *
 * ```kotlin
 * val inRange = queryParams.isDateInRange(LocalDateTime.now())
 * // inRange == true when date is within the configured range
 * ```
 */
fun QueryParams.isDateInRange(date: LocalDateTime): Boolean {
    if (from().getOrNull()?.isAfter(date) == true) {
        return false
    }
    if (to().getOrNull()?.isBefore(date) == true) {
        return false
    }
    return true
}
