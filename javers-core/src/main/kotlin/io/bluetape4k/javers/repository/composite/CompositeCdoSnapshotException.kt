package io.bluetape4k.javers.repository.composite

import io.bluetape4k.support.requireNotEmpty

/**
 * [CompositeCdoSnapshotRepository]에서 발생한 aggregate exception입니다.
 *
 * ## 동작 / 계약
 * - [failures]는 비어 있지 않습니다.
 * - 첫 failure를 exception cause로 노출합니다.
 * - message에는 delegate kind, index, class name, operation만 포함합니다.
 *   raw snapshot global-id 값이나 payload content는 의도적으로 포함하지 않습니다.
 */
class CompositeCdoSnapshotException private constructor(
    val failures: List<CompositeCdoSnapshotWriteFailure>,
    message: String,
    cause: Throwable,
): RuntimeException(message, cause) {

    /**
     * 하나 이상의 delegate failure로 aggregate exception을 생성합니다.
     */
    constructor(failures: List<CompositeCdoSnapshotWriteFailure>): this(buildPayload(failures))

    private constructor(payload: FailurePayload): this(
        failures = payload.failures,
        message = payload.message,
        cause = payload.cause,
    )

    companion object {
        private const val serialVersionUID: Long = -6040612489048914146L

        private fun buildPayload(failures: List<CompositeCdoSnapshotWriteFailure>): FailurePayload {
            failures.requireNotEmpty("failures")
            val copiedFailures = failures.toList()
            return FailurePayload(
                failures = copiedFailures,
                message = buildMessage(copiedFailures),
                cause = copiedFailures.first().cause,
            )
        }

        private fun buildMessage(failures: List<CompositeCdoSnapshotWriteFailure>): String {
            val details = failures.joinToString(separator = "; ") { failure ->
                "${failure.delegateKind}#${failure.delegateIndex} " +
                    "${failure.delegateClassName}.${failure.operation}: ${failure.cause::class.java.simpleName}"
            }
            return "Composite CDO snapshot repository operation failed. failures=${failures.size}; $details"
        }
    }

    private data class FailurePayload(
        val failures: List<CompositeCdoSnapshotWriteFailure>,
        val message: String,
        val cause: Throwable,
    )
}
