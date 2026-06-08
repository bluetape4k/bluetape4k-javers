package io.bluetape4k.javers.repository.composite

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.Serializable

/**
 * Describes one failed delegate operation in [CompositeCdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - [delegateIndex] is zero-based within [delegateKind].
 * - [delegateClassName] and [operation] are safe diagnostic values and must not
 *   contain raw snapshot identifiers.
 * - [cause] is the original delegate failure.
 */
@ConsistentCopyVisibility
data class CompositeCdoSnapshotWriteFailure private constructor(
    val delegateKind: CompositeCdoSnapshotDelegateKind,
    val delegateIndex: Int,
    val delegateClassName: String,
    val operation: String,
    val cause: Throwable,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 4872384723984723984L

        /**
         * Creates a validated delegate failure descriptor.
         */
        operator fun invoke(
            delegateKind: CompositeCdoSnapshotDelegateKind,
            delegateIndex: Int,
            delegateClassName: String,
            operation: String,
            cause: Throwable,
        ): CompositeCdoSnapshotWriteFailure {
            delegateIndex.requireZeroOrPositiveNumber("delegateIndex")
            delegateClassName.requireNotBlank("delegateClassName")
            operation.requireNotBlank("operation")

            return CompositeCdoSnapshotWriteFailure(
                delegateKind = delegateKind,
                delegateIndex = delegateIndex,
                delegateClassName = delegateClassName,
                operation = operation,
                cause = cause,
            )
        }
    }
}
