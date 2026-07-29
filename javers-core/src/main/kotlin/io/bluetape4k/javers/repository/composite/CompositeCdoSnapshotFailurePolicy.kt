package io.bluetape4k.javers.repository.composite

/**
 * secondary composite repository operation의 failure handling policy입니다.
 */
enum class CompositeCdoSnapshotFailurePolicy {

    /**
     * 실패한 첫 secondary delegate에서 중단하고 failure를 전파합니다.
     */
    FAIL_FAST,

    /**
     * 모든 secondary delegate를 시도한 뒤 모든 failure를 보고합니다.
     */
    BEST_EFFORT,
}
