package io.bluetape4k.javers.base

/**
 * Enumerates entity state change event types.
 *
 * - [UNKNOWN]: unknown event
 * - [SAVED]: entity creation or update
 * - [DELETED]: entity deletion
 *
 * ```kotlin
 * val type = EntityEventType.SAVED
 * // type.status == "SAVED"
 * ```
 */
enum class EntityEventType(val status: String) {

    /** Unknown event. */
    UNKNOWN("UNKNOWN"),

    /** Entity creation or update. */
    SAVED("SAVED"),

    /** Entity deletion. */
    DELETED("DELETED");

    override fun toString(): String = status

    companion object {
        /**
         * Returns the [EntityEventType] matching [status], or `null` when no type matches.
         */
        fun valueOf(status: String): EntityEventType? {
            return entries.firstOrNull { it.status == status }
        }
    }
}
