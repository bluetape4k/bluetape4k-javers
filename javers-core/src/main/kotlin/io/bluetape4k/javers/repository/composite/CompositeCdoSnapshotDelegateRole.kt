package io.bluetape4k.javers.repository.composite

/**
 * Role of a delegate repository inside [CompositeCdoSnapshotRepository].
 */
enum class CompositeCdoSnapshotDelegateRole {

    /**
     * Durable read/write source of truth.
     */
    PRIMARY,

    /**
     * Ordered fanout target such as Kafka, Redis, or another projection store.
     */
    SECONDARY,
}
