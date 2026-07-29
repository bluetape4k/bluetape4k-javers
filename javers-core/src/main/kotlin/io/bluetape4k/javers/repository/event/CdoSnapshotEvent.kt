package io.bluetape4k.javers.repository.event

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.io.Serializable
import java.time.Instant

/**
 * [CdoSnapshotEventMetadata.codecId]에서 사용하는 stable codec identifier입니다.
 */
object CdoSnapshotEventCodecIds {

    /**
     * [JaversCodecs.String]으로 encode한 plain JaVers JSON string payload입니다.
     */
    const val JSON_STRING: String = "javers:string-json"
}

/**
 * JaVers [CdoSnapshot] event를 위한 transport-neutral metadata입니다.
 *
 * ## 동작 / 계약
 * - [globalIdValue], [commitId], [snapshotType], [codecId], [idempotencyKey]는 non-blank입니다.
 * - [commitMajorId]는 positive이고 [commitMinorId]는 zero-or-positive입니다.
 * - [snapshotVersion]은 positive입니다.
 * - `AbstractCdoSnapshotRepository`가 `saveSnapshot()` 성공 후 sequence를 배정하므로
 *   [repositorySequence]는 nullable입니다.
 * - [idempotencyKey]는 opaque입니다. Transport가 deduplication에 사용할 수 있지만 parse하면 안 됩니다.
 *
 * ```kotlin
 * val metadata = CdoSnapshotEventMetadata.from(snapshot, CdoSnapshotEventCodecIds.JSON_STRING)
 * val event = CdoSnapshotEvent(metadata, encodedJson)
 * ```
 */
@ConsistentCopyVisibility
data class CdoSnapshotEventMetadata private constructor(
    val globalIdValue: String,
    val commitId: String,
    val commitMajorId: Long,
    val commitMinorId: Int,
    val repositorySequence: Long?,
    val snapshotVersion: Long,
    val snapshotType: String,
    val author: String,
    val commitTimestamp: Instant,
    val codecId: String,
    val idempotencyKey: String,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1649948814417484804L

        /**
         * validation된 transport-neutral snapshot event metadata를 생성합니다.
         */
        operator fun invoke(
            globalIdValue: String,
            commitId: String,
            commitMajorId: Long,
            commitMinorId: Int,
            repositorySequence: Long?,
            snapshotVersion: Long,
            snapshotType: String,
            author: String,
            commitTimestamp: Instant,
            codecId: String,
            idempotencyKey: String,
        ): CdoSnapshotEventMetadata {
            globalIdValue.requireNotBlank("globalIdValue")
            commitId.requireNotBlank("commitId")
            snapshotType.requireNotBlank("snapshotType")
            codecId.requireNotBlank("codecId")
            idempotencyKey.requireNotBlank("idempotencyKey")
            commitMajorId.requirePositiveNumber("commitMajorId")
            commitMinorId.requireZeroOrPositiveNumber("commitMinorId")
            snapshotVersion.requirePositiveNumber("snapshotVersion")
            repositorySequence?.requireZeroOrPositiveNumber("repositorySequence")

            return CdoSnapshotEventMetadata(
                globalIdValue = globalIdValue,
                commitId = commitId,
                commitMajorId = commitMajorId,
                commitMinorId = commitMinorId,
                repositorySequence = repositorySequence,
                snapshotVersion = snapshotVersion,
                snapshotType = snapshotType,
                author = author,
                commitTimestamp = commitTimestamp,
                codecId = codecId,
                idempotencyKey = idempotencyKey,
            )
        }

        /**
         * JaVers [snapshot]에서 metadata를 생성합니다.
         */
        fun from(
            snapshot: CdoSnapshot,
            codecId: String,
            repositorySequence: Long? = null,
            idempotencyKey: String = defaultIdempotencyKey(snapshot),
        ): CdoSnapshotEventMetadata {
            val commitId = snapshot.commitId
            return CdoSnapshotEventMetadata(
                globalIdValue = snapshot.globalId.value(),
                commitId = commitId.value(),
                commitMajorId = commitId.majorId,
                commitMinorId = commitId.minorId,
                repositorySequence = repositorySequence,
                snapshotVersion = snapshot.version,
                snapshotType = snapshot.type.name,
                author = snapshot.commitMetadata.author,
                commitTimestamp = snapshot.commitMetadata.commitDateInstant,
                codecId = codecId,
                idempotencyKey = idempotencyKey,
            )
        }

        /**
         * 같은 global id, commit id, snapshot version에 대해 stable opaque key를 반환합니다.
         */
        fun defaultIdempotencyKey(snapshot: CdoSnapshot): String =
            "${snapshot.globalId.value()}:${snapshot.commitId.value()}:${snapshot.version}"
    }
}

/**
 * transport-specific publisher에 전달할 준비가 된 encode된 JaVers snapshot event입니다.
 *
 * @param T encode된 payload type입니다.
 * @property metadata transport-neutral snapshot metadata입니다.
 * @property payload encode된 snapshot payload입니다.
 */
@ConsistentCopyVisibility
data class CdoSnapshotEvent<T: Any> private constructor(
    val metadata: CdoSnapshotEventMetadata,
    val payload: T,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = -7275910613949652842L

        /**
         * encode된 snapshot event를 생성합니다.
         */
        operator fun <T: Any> invoke(
            metadata: CdoSnapshotEventMetadata,
            payload: T,
        ): CdoSnapshotEvent<T> =
            CdoSnapshotEvent(metadata, payload)
    }
}

/**
 * encode된 JaVers snapshot event를 위한 synchronous publisher contract입니다.
 *
 * ## 동작 / 계약
 * - [publish]는 transport가 [event]를 accept 또는 acknowledge한 뒤에만 반환합니다.
 * - Publish failure는 caller로 전파합니다.
 * - [InterruptedException]을 catch하는 구현은 interrupt status를 복원해야 합니다.
 */
fun interface CdoSnapshotEventPublisher<T: Any> {

    /**
     * [event]를 publish하며, transport가 accept할 수 없으면 예외를 던집니다.
     */
    fun publish(event: CdoSnapshotEvent<T>)
}
