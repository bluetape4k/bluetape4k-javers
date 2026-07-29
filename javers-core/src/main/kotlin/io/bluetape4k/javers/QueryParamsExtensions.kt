package io.bluetape4k.javers

import org.javers.repository.api.QueryParams
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

/**
 * [date]가 이 [QueryParams]의 from-to 범위 안에 있으면 `true`를 반환합니다.
 *
 * ## 동작 / 계약
 * - `from`이 설정되어 있고 [date]보다 뒤라면 `false`를 반환합니다.
 * - `to`가 설정되어 있고 [date]보다 앞이라면 `false`를 반환합니다.
 * - 설정되지 않은 경계는 제한이 없는 것으로 취급합니다.
 *
 * ```kotlin
 * val inRange = queryParams.isDateInRange(LocalDateTime.now())
 * // date가 설정된 범위 안에 있으면 inRange == true
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
