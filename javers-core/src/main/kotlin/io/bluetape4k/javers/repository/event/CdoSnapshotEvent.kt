package io.bluetape4k.javers.repository.event

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.io.Serializable
import java.time.Instant

/**
 * Stable codec identifiers used in [CdoSnapshotEventMetadata.codecId].
 */
object CdoSnapshotEventCodecIds {

    /**
     * Plain JaVers JSON string payload encoded with [JaversCodecs.String].
     */
    const val JSON_STRING: String = "javers:string-json"
}

/**
 * Transport-neutral metadata for a JaVers [CdoSnapshot] event.
 *
 * ## Behavior / Contract
 * - [globalIdValue], [commitId], [snapshotType], [codecId], and [idempotencyKey]
 *   are non-blank.
 * - [commitMajorId] is positive and [commitMinorId] is zero or positive.
 * - [snapshotVersion] is positive.
 * - [repositorySequence] is nullable because `AbstractCdoSnapshotRepository`
 *   assigns its sequence after `saveSnapshot()` succeeds.
 * - [idempotencyKey] is opaque. Transports may use it for deduplication, but
 *   should not parse it.
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
         * Creates validated transport-neutral snapshot event metadata.
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
         * Builds metadata from a JaVers [snapshot].
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
         * Returns a stable opaque key for the same global id, commit id, and snapshot version.
         */
        fun defaultIdempotencyKey(snapshot: CdoSnapshot): String =
            "${snapshot.globalId.value()}:${snapshot.commitId.value()}:${snapshot.version}"
    }
}

/**
 * Encoded JaVers snapshot event ready for a transport-specific publisher.
 *
 * @param T encoded payload type
 * @property metadata transport-neutral snapshot metadata
 * @property payload encoded snapshot payload
 */
@ConsistentCopyVisibility
data class CdoSnapshotEvent<T: Any> private constructor(
    val metadata: CdoSnapshotEventMetadata,
    val payload: T,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = -7275910613949652842L

        /**
         * Creates an encoded snapshot event.
         */
        operator fun <T: Any> invoke(
            metadata: CdoSnapshotEventMetadata,
            payload: T,
        ): CdoSnapshotEvent<T> =
            CdoSnapshotEvent(metadata, payload)
    }
}

/**
 * Synchronous publisher contract for encoded JaVers snapshot events.
 *
 * ## Behavior / Contract
 * - [publish] returns only after the transport accepts or acknowledges [event].
 * - Publish failures are propagated to the caller.
 * - Implementations that catch [InterruptedException] must restore interrupt status.
 */
fun interface CdoSnapshotEventPublisher<T: Any> {

    /**
     * Publishes [event] or throws when the transport cannot accept it.
     */
    fun publish(event: CdoSnapshotEvent<T>)
}
