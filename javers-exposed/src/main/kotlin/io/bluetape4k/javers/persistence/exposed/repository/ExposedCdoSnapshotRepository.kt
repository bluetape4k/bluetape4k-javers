package io.bluetape4k.javers.persistence.exposed.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTableMapping
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTableMapping
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversSchema
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
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
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.Serializable

/**
 * Options for [ExposedCdoSnapshotRepository].
 *
 * ## Contract
 * Defaults preserve the original `javers_commit` / `javers_snapshot` table
 * mapping and keep [ensureSchema] creating those tables. Set
 * [createSchemaOnEnsure] to `false` when schema migrations are owned by an
 * external migration tool.
 *
 * ```kotlin
 * val options = ExposedCdoSnapshotRepositoryOptions(
 *     tableNames = ExposedJaversTableNames(
 *         commitTableName = "audit_commit",
 *         snapshotTableName = "audit_snapshot",
 *     ),
 *     createSchemaOnEnsure = false,
 * )
 * ```
 */
data class ExposedCdoSnapshotRepositoryOptions(
    val tableNames: ExposedJaversTableNames = ExposedJaversTableNames.Default,
    val createSchemaOnEnsure: Boolean = true,
): Serializable {

    fun newSchema(): ExposedJaversSchema = ExposedJaversSchema.from(tableNames)

    companion object {
        private const val serialVersionUID: Long = 1L

        val Default: ExposedCdoSnapshotRepositoryOptions = ExposedCdoSnapshotRepositoryOptions()
    }
}

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
 * - Uses [options] to customize table names and schema creation ownership.
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
 * @property options table mapping and schema initialization options
 */
class ExposedCdoSnapshotRepository(
    private val database: Database? = null,
    codec: JaversCodec<String> = JaversCodecs.String,
    private val options: ExposedCdoSnapshotRepositoryOptions = ExposedCdoSnapshotRepositoryOptions.Default,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val schema: ExposedJaversSchema = options.newSchema()
    private val commitTable: CommitTableMapping = schema.commitTable
    private val snapshotTable: CdoSnapshotTableMapping = schema.snapshotTable

    override fun ensureSchema() {
        if (!options.createSchemaOnEnsure) {
            return
        }
        inTransaction {
            SchemaUtils.create(*schema.tables)
        }
    }

    override fun getKeys(): Set<String> = inTransaction {
        snapshotTable
            .selectAll()
            .map { it[snapshotTable.globalId] }
            .toSet()
    }

    override fun contains(globalIdValue: String): Boolean = inTransaction {
        snapshotTable
            .selectAll()
            .where { snapshotTable.globalId eq globalIdValue }
            .limit(1)
            .empty()
            .not()
    }

    override fun getSeq(commitId: CommitId): Long = inTransaction {
        commitTable
            .selectAll()
            .where { commitTable.commitId eq commitId.value() }
            .singleOrNull()
            ?.get(commitTable.sequence)
            ?: 0L
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        inTransaction {
            val updated = commitTable.update({ commitTable.commitId eq commitId.value() }) {
                it[commitTable.sequence] = sequence
            }
            log.trace { "Updated commit sequence. commitId=${commitId.value()}, sequence=$sequence, updated=$updated" }
        }
    }

    override fun loadHeadId(): CommitId? = inTransaction {
        commitTable
            .selectAll()
            .orderBy(commitTable.sequence, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(commitTable.commitId)
            ?.let { CommitId.valueOf(it) }
    }

    override fun getSnapshotSize(globalIdValue: String): Int = inTransaction {
        snapshotTable
            .selectAll()
            .where { snapshotTable.globalId eq globalIdValue }
            .count()
            .toInt()
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val encodedSnapshot = encode(snapshot)
        val metadata = snapshot.commitMetadata
        inTransaction {
            saveCommitMetadataIfAbsent(snapshot)

            snapshotTable.insert {
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

    private fun saveCommitMetadataIfAbsent(snapshot: CdoSnapshot) {
        val metadata = snapshot.commitMetadata
        val commitIdValue = metadata.id.value()
        val exists = commitTable
            .selectAll()
            .where { commitTable.commitId eq commitIdValue }
            .limit(1)
            .empty()
            .not()

        if (exists) {
            return
        }

        commitTable.insert {
            it[commitId] = commitIdValue
            it[author] = metadata.author
            it[commitDate] = metadata.commitDate
            it[commitDateInstant] = metadata.commitDateInstant?.toString()
            it[properties] = metadata.properties.toJsonObject().toString()
            it[sequence] = 0L
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> = inTransaction {
        snapshotTable
            .selectAll()
            .where { snapshotTable.globalId eq globalIdValue }
            .orderBy(snapshotTable.version, SortOrder.DESC)
            .mapNotNull { decode(it[snapshotTable.state]) }
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
