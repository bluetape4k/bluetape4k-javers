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
     * 이미 저장된 [snapshot]을 replay에서 건너뛰기 전에 metadata를 검증합니다.
     *
     * 기본 구현은 metadata를 소유하지 않는 snapshot-only 저장소를 위한 no-op입니다.
     * commit sequence와 head를 관리하는 구현은 불완전한 상태에서 예외를 발생시켜야 합니다.
     * 이 검증은 snapshot row를 다시 저장하지 않습니다.
     *
     * @throws IllegalStateException 저장된 metadata로 안전한 replay를 보장할 수 없는 경우
     */
    fun validateSnapshotMetadata(snapshot: CdoSnapshot) = Unit

    /**
     * 지정한 GlobalId 값의 snapshot을 반환합니다.
     */
    fun loadSnapshots(globalIdValue: String): List<CdoSnapshot>

    /**
     * 지정한 [GlobalId]의 snapshot을 반환합니다.
     */
    fun loadSnapshots(globalId: GlobalId): List<CdoSnapshot> = loadSnapshots(globalId.value())
}
