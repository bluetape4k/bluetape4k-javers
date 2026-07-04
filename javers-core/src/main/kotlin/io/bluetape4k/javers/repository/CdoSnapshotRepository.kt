package io.bluetape4k.javers.repository

import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.repository.api.JaversRepository

/**
 * JaVers repository interface for storing and loading [CdoSnapshot] values.
 *
 * ## Contract
 * - Stores a single snapshot with [saveSnapshot].
 * - Projects a decoded snapshot with [projectSnapshot]; repositories that track
 *   commit metadata should restore head and sequence state there.
 * - Loads snapshots for a GlobalId with [loadSnapshots] in newest-first order.
 *
 * ```kotlin
 * val repo: CdoSnapshotRepository = CaffeineCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 */
interface CdoSnapshotRepository: JaversRepository {

    /**
     * Stores a [CdoSnapshot] in the repository.
     */
    fun saveSnapshot(snapshot: CdoSnapshot)

    /**
     * Projects a decoded [CdoSnapshot] into the repository during replay.
     *
     * The default implementation is snapshot-only for simple repositories.
     * Durable repositories should override this method or inherit
     * `AbstractCdoSnapshotRepository` so replay also restores commit head and
     * sequence metadata.
     */
    fun projectSnapshot(snapshot: CdoSnapshot) {
        saveSnapshot(snapshot)
    }

    /**
     * Returns snapshots for the specified GlobalId value.
     */
    fun loadSnapshots(globalIdValue: String): List<CdoSnapshot>

    /**
     * Returns snapshots for the specified [GlobalId].
     */
    fun loadSnapshots(globalId: GlobalId): List<CdoSnapshot> = loadSnapshots(globalId.value())
}
