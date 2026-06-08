package io.bluetape4k.javers.repository.composite

import java.io.Serializable

/**
 * Options for [CompositeCdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * - [writeFailurePolicy] controls secondary `saveSnapshot` and `persist`
 *   failures after the primary has succeeded.
 * - [ensureSchemaFailurePolicy] controls secondary schema initialization
 *   failures after the primary has succeeded.
 * - [closeFailurePolicy] controls whether close stops on the first closeable
 *   delegate failure or attempts every closeable delegate.
 */
@ConsistentCopyVisibility
data class CompositeCdoSnapshotRepositoryOptions private constructor(
    val writeFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.FAIL_FAST,
    val ensureSchemaFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.FAIL_FAST,
    val closeFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.BEST_EFFORT,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 5379184729384729384L

        /**
         * Default composite repository options.
         */
        val Default: CompositeCdoSnapshotRepositoryOptions = CompositeCdoSnapshotRepositoryOptions()

        /**
         * Creates composite repository options.
         */
        operator fun invoke(
            writeFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.FAIL_FAST,
            ensureSchemaFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.FAIL_FAST,
            closeFailurePolicy: CompositeCdoSnapshotFailurePolicy = CompositeCdoSnapshotFailurePolicy.BEST_EFFORT,
        ): CompositeCdoSnapshotRepositoryOptions =
            CompositeCdoSnapshotRepositoryOptions(
                writeFailurePolicy = writeFailurePolicy,
                ensureSchemaFailurePolicy = ensureSchemaFailurePolicy,
                closeFailurePolicy = closeFailurePolicy,
            )
    }
}
