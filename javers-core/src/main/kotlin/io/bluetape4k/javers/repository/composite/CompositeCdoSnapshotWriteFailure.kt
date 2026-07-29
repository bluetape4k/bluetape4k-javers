package io.bluetape4k.javers.repository.composite

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.Serializable

/**
 * [CompositeCdoSnapshotRepository]의 실패한 delegate operation 하나를 설명합니다.
 *
 * ## 동작 / 계약
 * - [delegateIndex]는 [delegateKind] 안에서 zero-based입니다.
 * - [delegateClassName]과 [operation]은 안전한 diagnostic value이며 raw snapshot identifier를 포함하면 안 됩니다.
 * - [cause]는 원본 delegate failure입니다.
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
         * validation된 delegate failure descriptor를 생성합니다.
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
