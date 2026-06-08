package io.bluetape4k.javers.repository.composite

import io.bluetape4k.support.requireNotEmpty

/**
 * Aggregate exception raised by [CompositeCdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - [failures] is never empty.
 * - The first failure is exposed as the exception cause.
 * - The message includes delegate role, index, type, and operation only. It
 *   intentionally avoids raw snapshot global-id values or payload content.
 */
class CompositeCdoSnapshotException private constructor(
    val failures: List<CompositeCdoSnapshotWriteFailure>,
    message: String,
    cause: Throwable,
): RuntimeException(message, cause) {

    /**
     * Creates an aggregate exception from one or more delegate failures.
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
                "${failure.delegateRole}#${failure.delegateIndex} " +
                    "${failure.delegateType}.${failure.operation}: ${failure.cause::class.java.simpleName}"
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
