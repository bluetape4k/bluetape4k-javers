package io.bluetape4k.javers.repository.event

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test
import java.time.Instant

class CdoSnapshotEventTest {

    @Test
    fun `metadata from snapshot exposes transport neutral fields`() {
        val snapshot = JaversBuilder.javers()
            .build()
            .commit("event-author", SnapshotEntity(1))
            .snapshots
            .first()

        val metadata = CdoSnapshotEventMetadata.from(snapshot, CdoSnapshotEventCodecIds.JSON_STRING)

        metadata.globalIdValue shouldBeEqualTo "org.javers.core.model.SnapshotEntity/1"
        metadata.commitId shouldBeEqualTo snapshot.commitId.value()
        metadata.commitMajorId shouldBeEqualTo snapshot.commitId.majorId
        metadata.commitMinorId shouldBeEqualTo snapshot.commitId.minorId
        metadata.repositorySequence.shouldBeNull()
        metadata.snapshotVersion shouldBeEqualTo 1L
        metadata.snapshotType shouldBeEqualTo SnapshotType.INITIAL.name
        metadata.author shouldBeEqualTo "event-author"
        metadata.commitTimestamp shouldBeEqualTo snapshot.commitMetadata.commitDateInstant
        metadata.codecId shouldBeEqualTo CdoSnapshotEventCodecIds.JSON_STRING
        metadata.idempotencyKey shouldBeEqualTo CdoSnapshotEventMetadata.defaultIdempotencyKey(snapshot)
    }

    @Test
    fun `metadata validates required fields through companion invoke`() {
        assertFailsWith<IllegalArgumentException> {
            metadata(globalIdValue = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            metadata(commitId = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            metadata(snapshotVersion = 0L)
        }
        assertFailsWith<IllegalArgumentException> {
            metadata(repositorySequence = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            metadata(codecId = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            metadata(idempotencyKey = " ")
        }
    }

    @Test
    fun `event carries metadata and encoded payload`() {
        val metadata = metadata()
        val event = CdoSnapshotEvent(metadata, "encoded-snapshot")

        event.metadata shouldBeEqualTo metadata
        event.payload shouldBeEqualTo "encoded-snapshot"
    }

    private fun metadata(
        globalIdValue: String = "Entity/1",
        commitId: String = "1.00",
        repositorySequence: Long? = null,
        snapshotVersion: Long = 1L,
        codecId: String = CdoSnapshotEventCodecIds.JSON_STRING,
        idempotencyKey: String = "Entity/1:1.00:1",
    ): CdoSnapshotEventMetadata =
        CdoSnapshotEventMetadata(
            globalIdValue = globalIdValue,
            commitId = commitId,
            commitMajorId = 1L,
            commitMinorId = 0,
            repositorySequence = repositorySequence,
            snapshotVersion = snapshotVersion,
            snapshotType = SnapshotType.INITIAL.name,
            author = "author",
            commitTimestamp = Instant.EPOCH,
            codecId = codecId,
            idempotencyKey = idempotencyKey,
        )
}
