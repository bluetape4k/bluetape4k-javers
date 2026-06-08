package io.bluetape4k.javers.repository.composite

/**
 * Failure handling policy for secondary composite repository operations.
 */
enum class CompositeCdoSnapshotFailurePolicy {

    /**
     * Stop at the first failed secondary delegate and propagate the failure.
     */
    FAIL_FAST,

    /**
     * Attempt every secondary delegate and report all failures afterward.
     */
    BEST_EFFORT,
}
