package io.bluetape4k.javers.metamodel

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.javers.getWrappedOrNull
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test

class CdoSnapshotSupportTest {

    @Test
    fun `snapshot property helpers expose state entries`() {
        val snapshot = JaversBuilder.javers()
            .build()
            .commit("author", SnapshotEntity(1).apply { intProperty = 7 })
            .snapshots
            .first()

        val mapped = snapshot.mapProperties { key, value -> key to value }.toMap()
        val visited = mutableListOf<String>()
        snapshot.forEachProperties<Unit> { key, _ -> visited += key }

        mapped["id"] shouldBeEqualTo 1
        mapped["intProperty"] shouldBeEqualTo 7
        visited shouldContain "id"
        snapshot.getWrappedOrNull() shouldBeEqualTo snapshot.getWrappedCdo().orElse(null)
    }

    @Test
    fun `snapshot sequence filters cover commit id version author changed property type and commit properties`() {
        val javers = JaversBuilder.javers().build()
        val entity = SnapshotEntity(1)
        javers.commit("alice", entity, mapOf("tenant" to "blue"))
        entity.intProperty = 10
        javers.commit("bob", entity, mapOf("tenant" to "red"))
        entity.dob = java.time.LocalDate.of(2026, 7, 5)
        javers.commit("alice", entity, mapOf("tenant" to "blue"))

        val snapshots = javers.findSnapshots(queryByInstanceId<SnapshotEntity>(1))
        val newest = snapshots[0]
        val middle = snapshots[1]
        val oldest = snapshots[2]

        snapshots.asSequence().filterByToCommitId(middle.commitId).toList() shouldContainSame listOf(middle, oldest)
        snapshots.asSequence().filterByCommitIds(listOf(newest.commitId, oldest.commitId)).toList() shouldContainSame
            listOf(newest, oldest)
        snapshots.asSequence().filterByVersion(2L).toList() shouldBeEqualTo listOf(middle)
        snapshots.asSequence().filterByAuthor("alice").toList() shouldContainSame listOf(newest, oldest)
        snapshots.asSequence().filterByChangedPropertyName("intProperty").toList() shouldBeEqualTo listOf(middle)
        snapshots.asSequence().filterByChangedPropertyNames(setOf("dob", "missing")).toList() shouldBeEqualTo
            listOf(newest)
        snapshots.asSequence().filterByType(SnapshotType.UPDATE).toList() shouldContainSame listOf(newest, middle)
        snapshots.asSequence().filterByCommitProperties(mapOf("tenant" to listOf("blue"))).toList() shouldContainSame
            listOf(newest, oldest)
        snapshots.asSequence().trimToRequestedSlice(skip = 1, limit = 1) shouldHaveSize 1
    }
}
