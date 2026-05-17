package org.javers.core.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import org.javers.core.Javers
import org.javers.core.model.CategoryC
import org.javers.core.model.PhoneWithShallowCategory
import org.javers.core.model.ShallowPhone
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test

abstract class AbstractJaversCommitTest {

    companion object: KLogging()

    abstract fun newJavers(): Javers

    @Test
    fun `ShallowReferenceType entity snapshot is not committed`() {
        val javers = newJavers()
        val reference = ShallowPhone(1L, "123", CategoryC(1, "some"))
        val entity = SnapshotEntity(id = 1).apply {
            shallowPhone = reference
            shallowPhones = mutableSetOf(reference)
            shallowPhonesList = mutableListOf(reference)
            shallowPhonesMap = mutableMapOf("key" to reference)
        }

        // WHEN
        var commit = javers.commit("", entity)

        // THEN: initial commit captures the entity snapshot
        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.size shouldBeEqualTo 1

        // Changing a shallow reference should not produce a new snapshot
        reference.number = "other"

        commit = javers.commit("", entity)

        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots shouldBeEqualTo emptyList()
    }

    @Test
    fun `changes in a property annotated with @ShallowReference are not committed as a snapshot`() {
        val javers = newJavers()
        val entity = PhoneWithShallowCategory(1).apply {
            shallowCategory = CategoryC(1, "old shallow")
        }

        var commit = javers.commit("", entity)

        // THEN: initial commit captures the entity snapshot
        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots.size shouldBeEqualTo 1

        // Changing a @ShallowReference property should not produce a new snapshot
        entity.shallowCategory?.name = "new shallow"

        commit = javers.commit("", entity)

        commit.snapshots.forEach { log.debug { it } }
        commit.snapshots shouldBeEqualTo emptyList()
    }
}
