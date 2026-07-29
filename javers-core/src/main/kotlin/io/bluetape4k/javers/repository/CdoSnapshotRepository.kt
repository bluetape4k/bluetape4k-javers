package io.bluetape4k.javers.repository

import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.repository.api.JaversRepository

/**
 * [CdoSnapshot] 값을 저장하고 load하는 JaVers repository interface입니다.
 *
 * ## 계약
 * - [saveSnapshot]으로 단일 snapshot을 저장합니다.
 * - [projectSnapshot]으로 decode된 snapshot을 project합니다. commit metadata를 추적하는 repository는
 *   여기서 head와 sequence state를 복원해야 합니다.
 * - [loadSnapshots]로 GlobalId의 snapshot을 newest-first 순서로 load합니다.
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
     * repository에 [CdoSnapshot]을 저장합니다.
     */
    fun saveSnapshot(snapshot: CdoSnapshot)

    /**
     * replay 중 decode된 [CdoSnapshot]을 repository로 project합니다.
     *
     * 기본 구현은 단순 repository를 위한 snapshot-only 동작입니다.
     * Durable repository는 replay가 commit head와 sequence metadata도 복원하도록
     * 이 method를 override하거나 `AbstractCdoSnapshotRepository`를 상속해야 합니다.
     */
    fun projectSnapshot(snapshot: CdoSnapshot) {
        saveSnapshot(snapshot)
    }

    /**
     * 지정한 GlobalId 값의 snapshot을 반환합니다.
     */
    fun loadSnapshots(globalIdValue: String): List<CdoSnapshot>

    /**
     * 지정한 [GlobalId]의 snapshot을 반환합니다.
     */
    fun loadSnapshots(globalId: GlobalId): List<CdoSnapshot> = loadSnapshots(globalId.value())
}
