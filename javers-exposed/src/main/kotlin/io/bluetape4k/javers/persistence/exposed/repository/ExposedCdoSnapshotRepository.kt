package io.bluetape4k.javers.persistence.exposed.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * JaVers CDO snapshot repository backed by Exposed JDBC.
 *
 * ## Contract
 * - Stores one row per [CdoSnapshot] in [CdoSnapshotTable].
 * - Stores one row per JaVers commit in [CommitTable].
 * - Encodes the full snapshot JSON with [codec], allowing JaVers to reconstruct
 *   snapshots through its configured JSON converter.
 * - Uses Exposed `transaction {}` or `transaction(database) {}` for every
 *   operation and never manages JDBC commits directly.
 *
 * ```kotlin
 * val repository = ExposedCdoSnapshotRepository(database)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repository)
 *     .build()
 * repository.ensureSchema()
 * ```
 *
 * @property database optional Exposed database used to route transactions
 * @property codec codec used to persist JaVers snapshot JSON
 */
class ExposedCdoSnapshotRepository(
    private val database: Database? = null,
    codec: JaversCodec<String> = JaversCodecs.String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    override fun ensureSchema() {
        inTransaction {
            SchemaUtils.create(CommitTable, CdoSnapshotTable)
        }
    }

    override fun getKeys(): Set<String> = inTransaction {
        CdoSnapshotTable
            .selectAll()
            .map { it[CdoSnapshotTable.globalId] }
            .toSet()
    }

    override fun contains(globalIdValue: String): Boolean = inTransaction {
        CdoSnapshotTable
            .selectAll()
            .where { CdoSnapshotTable.globalId eq globalIdValue }
            .limit(1)
            .empty()
            .not()
    }

    override fun getSeq(commitId: CommitId): Long = inTransaction {
        CommitTable
            .selectAll()
            .where { CommitTable.commitId eq commitId.value() }
            .singleOrNull()
            ?.get(CommitTable.sequence)
            ?: 0L
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        inTransaction {
            val updated = CommitTable.update({ CommitTable.commitId eq commitId.value() }) {
                it[CommitTable.sequence] = sequence
            }
            log.trace { "Updated commit sequence. commitId=${commitId.value()}, sequence=$sequence, updated=$updated" }
        }
    }

    override fun loadHeadId(): CommitId? = inTransaction {
        CommitTable
            .selectAll()
            .orderBy(CommitTable.sequence, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(CommitTable.commitId)
            ?.let { CommitId.valueOf(it) }
    }

    override fun getSnapshotSize(globalIdValue: String): Int = inTransaction {
        CdoSnapshotTable
            .selectAll()
            .where { CdoSnapshotTable.globalId eq globalIdValue }
            .count()
            .toInt()
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val encodedSnapshot = encode(snapshot)
        val metadata = snapshot.commitMetadata
        inTransaction {
            CommitTable.insertIgnore {
                it[commitId] = metadata.id.value()
                it[author] = metadata.author
                it[commitDate] = metadata.commitDate
                it[commitDateInstant] = metadata.commitDateInstant?.toString()
                it[properties] = metadata.properties.toJsonObject().toString()
                it[sequence] = 0L
            }

            CdoSnapshotTable.insert {
                it[globalId] = snapshot.globalId.value()
                it[commitId] = metadata.id.value()
                it[version] = snapshot.version
                it[type] = snapshot.type.name
                it[state] = encodedSnapshot
                it[changedProperties] = snapshot.changed.toJsonArray().toString()
                it[managedType] = snapshot.managedType.name
            }
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> = inTransaction {
        CdoSnapshotTable
            .selectAll()
            .where { CdoSnapshotTable.globalId eq globalIdValue }
            .orderBy(CdoSnapshotTable.version, SortOrder.DESC)
            .mapNotNull { decode(it[CdoSnapshotTable.state]) }
    }

    private fun Map<String, String>.toJsonObject(): JsonObject {
        val jsonObject = JsonObject()
        forEach { (key, value) -> jsonObject.addProperty(key, value) }
        return jsonObject
    }

    private fun List<String>.toJsonArray(): JsonArray {
        val jsonArray = JsonArray()
        forEach(jsonArray::add)
        return jsonArray
    }

    private fun <T> inTransaction(statement: () -> T): T {
        return database?.let { transaction(it) { statement() } } ?: transaction { statement() }
    }
}
