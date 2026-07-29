package io.bluetape4k.javers.repository.composite

/**
 * [CompositeCdoSnapshotRepository] 내부 delegate repository의 kind입니다.
 *
 * ## 동작 / 계약
 * composite repository는 정확히 하나의 [PRIMARY] delegate와 0개 이상의 [SECONDARY] delegate를 가집니다.
 * 여러 secondary는 [SECONDARY] kind를 공유하며, deterministic execution order는
 * [CompositeCdoSnapshotWriteFailure.delegateIndex]로 표현합니다.
 */
enum class CompositeCdoSnapshotDelegateKind {

    /**
     * Durable read/write source of truth입니다.
     */
    PRIMARY,

    /**
     * Kafka, Redis 또는 다른 projection store 같은 순서 있는 fanout target 중 하나입니다.
     */
    SECONDARY,
}
