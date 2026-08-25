package io.bluetape4k.javers.commit

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.javers.core.commit.CommitId
import org.javers.core.commit.CommitMetadata
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CommitMetadataExtensionsTest {

    @Test
    fun `commit id exposes major and minor version pair`() {
        CommitId(42L, 7).version shouldBeEqualTo (42L to 7)
    }

    @Test
    fun `commit metadata compares by commit id and exposes epoch milliseconds`() {
        val instant = Instant.parse("2026-07-05T00:00:00Z")
        val first = metadata(CommitId(1L, 0), instant)
        val second = metadata(CommitId(2L, 0), instant.plusSeconds(1))

        (first < second).shouldBeTrue()
        (second > first).shouldBeTrue()
        first.commitTimestamp shouldBeEqualTo instant.toEpochMilli()
        second.commitTimestamp shouldBeGreaterThan first.commitTimestamp
    }

    private fun metadata(commitId: CommitId, instant: Instant): CommitMetadata =
        CommitMetadata(
            "author",
            mapOf("scope" to "coverage"),
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
            instant,
            commitId,
        )
}
