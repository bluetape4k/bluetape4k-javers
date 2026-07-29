package io.bluetape4k.javers.base

/**
 * entity state change event type을 열거합니다.
 *
 * - [UNKNOWN]: 알 수 없는 event
 * - [SAVED]: entity 생성 또는 수정
 * - [DELETED]: entity 삭제
 *
 * ```kotlin
 * val type = EntityEventType.SAVED
 * // type.status == "SAVED"
 * ```
 */
enum class EntityEventType(val status: String) {

    /** 알 수 없는 event입니다. */
    UNKNOWN("UNKNOWN"),

    /** entity 생성 또는 수정 event입니다. */
    SAVED("SAVED"),

    /** entity 삭제 event입니다. */
    DELETED("DELETED");

    override fun toString(): String = status

    companion object {
        /**
         * [status]와 일치하는 [EntityEventType]을 반환하며, 일치하는 type이 없으면 `null`을 반환합니다.
         */
        fun valueOf(status: String): EntityEventType? {
            return entries.firstOrNull { it.status == status }
        }
    }
}
