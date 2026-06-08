package io.bluetape4k.javers.repository.composite

/**
 * Role of a delegate repository inside [CompositeCdoSnapshotRepository].
 *
 * ## Behavior / Contract
 * A composite repository has exactly one [PRIMARY] delegate and zero or more
 * [SECONDARY] delegates. Multiple secondaries share the [SECONDARY] role; their
 * deterministic execution order is represented by
 * [CompositeCdoSnapshotWriteFailure.delegateIndex].
 */
enum class CompositeCdoSnapshotDelegateRole {

    /**
     * Durable read/write source of truth.
     */
    PRIMARY,

    /**
     * One of zero or more ordered fanout targets such as Kafka, Redis, or
     * another projection store.
     */
    SECONDARY,
}
