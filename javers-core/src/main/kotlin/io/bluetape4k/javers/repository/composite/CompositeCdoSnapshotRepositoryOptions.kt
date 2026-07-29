package io.bluetape4k.javers.repository.composite

import java.io.Serializable

/**
 * [CompositeCdoSnapshotRepository] option입니다.
 *
 * ## 동작 / 계약
 * - [writeFailurePolicy]는 primary 성공 후 secondary `saveSnapshot` 및 `persist` failure를 제어합니다.
 * - [ensureSchemaFailurePolicy]는 primary 성공 후 secondary schema initialization failure를 제어합니다.
 * - [closeFailurePolicy]는 close가 첫 closeable delegate failure에서 중단할지,
 *   모든 closeable delegate를 시도할지 제어합니다.
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
         * 기본 composite repository option입니다.
         */
        val Default: CompositeCdoSnapshotRepositoryOptions = CompositeCdoSnapshotRepositoryOptions()

        /**
         * composite repository option을 생성합니다.
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
