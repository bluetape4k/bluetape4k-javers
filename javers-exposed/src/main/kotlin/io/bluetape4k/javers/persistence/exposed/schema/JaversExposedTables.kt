package io.bluetape4k.javers.persistence.exposed.schema

import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.io.Serializable

/**
 * Stores encoded JaVers CDO snapshots for Exposed JDBC persistence.
 *
 * The [state] column contains the full encoded `CdoSnapshot` JSON payload
 * produced by JaVers. Other columns duplicate query-oriented metadata for
 * indexing, inspection, and future SQL pushdown.
 */
open class CdoSnapshotTableMapping(
    tableName: String = DEFAULT_TABLE_NAME,
    globalIdVersionIndexName: String = DEFAULT_GLOBAL_ID_VERSION_INDEX,
): Table(tableName.requireNotBlank("tableName")) {

    val globalId = varchar("global_id", 200)
    val commitId = varchar("commit_id", 50).index()
    val version = long("version")
    val type = varchar("type", 50).index()
    val state = text("state")
    val changedProperties = text("changed_properties")
    val managedType = varchar("managed_type", 200).index()

    override val primaryKey: PrimaryKey = PrimaryKey(
        globalId,
        version,
        name = globalIdVersionIndexName.requireNotBlank("globalIdVersionIndexName"),
    )

    companion object {
        const val DEFAULT_TABLE_NAME: String = "javers_snapshot"
        const val DEFAULT_GLOBAL_ID_VERSION_INDEX: String = "ux_javers_snapshot_global_id_version"
    }
}

object CdoSnapshotTable: CdoSnapshotTableMapping() {
    const val GLOBAL_ID_VERSION_INDEX: String = DEFAULT_GLOBAL_ID_VERSION_INDEX
}

/**
 * Stores JaVers commit metadata and repository sequence numbers.
 *
 * [sequence] is the repository-local ordering value used to restore the latest
 * `CommitId` after rebuilding a persistent repository instance.
 */
open class CommitTableMapping(
    tableName: String = DEFAULT_TABLE_NAME,
    commitIdIndexName: String = DEFAULT_COMMIT_ID_INDEX,
    sequenceIndexName: String = DEFAULT_SEQUENCE_INDEX,
): Table(tableName.requireNotBlank("tableName")) {

    val commitId = varchar("commit_id", 50)
    val author = varchar("author", 200)
    val commitDate = datetime("commit_date")
    val commitDateInstant = varchar("commit_date_instant", 64).nullable()
    val properties = text("properties")
    val sequence = long("sequence").default(0L).index(sequenceIndexName.requireNotBlank("sequenceIndexName"))

    override val primaryKey: PrimaryKey = PrimaryKey(
        commitId,
        name = commitIdIndexName.requireNotBlank("commitIdIndexName"),
    )

    companion object {
        const val DEFAULT_TABLE_NAME: String = "javers_commit"
        const val DEFAULT_COMMIT_ID_INDEX: String = "javers_commit_commit_id_unique"
        const val DEFAULT_SEQUENCE_INDEX: String = "ix_javers_commit_sequence"
    }
}

object CommitTable: CommitTableMapping() {
    const val COMMIT_ID_INDEX: String = DEFAULT_COMMIT_ID_INDEX
    const val SEQUENCE_INDEX: String = DEFAULT_SEQUENCE_INDEX
}

/**
 * Table names used by [ExposedJaversSchema].
 *
 * ## Contract
 * Defaults preserve the original `javers_commit` and `javers_snapshot` table
 * names. Use this named value object instead of passing two raw strings to
 * avoid commit/snapshot table ordering mistakes.
 */
@ConsistentCopyVisibility
data class ExposedJaversTableNames private constructor(
    val commitTableName: String = CommitTableMapping.DEFAULT_TABLE_NAME,
    val snapshotTableName: String = CdoSnapshotTableMapping.DEFAULT_TABLE_NAME,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L

        operator fun invoke(
            commitTableName: String = CommitTableMapping.DEFAULT_TABLE_NAME,
            snapshotTableName: String = CdoSnapshotTableMapping.DEFAULT_TABLE_NAME,
        ): ExposedJaversTableNames {
            return ExposedJaversTableNames(
                commitTableName = commitTableName.requireNotBlank("commitTableName"),
                snapshotTableName = snapshotTableName.requireNotBlank("snapshotTableName"),
            )
        }

        val Default: ExposedJaversTableNames = invoke()
    }
}

/**
 * Exposed table mapping pair used by `ExposedCdoSnapshotRepository`.
 *
 * ## Contract
 * [tables] is ordered for schema creation and drop helpers. Default schema uses
 * the source-compatible [CommitTable] and [CdoSnapshotTable] singleton objects.
 */
class ExposedJaversSchema(
    val commitTable: CommitTableMapping,
    val snapshotTable: CdoSnapshotTableMapping,
) {

    val tables: Array<Table>
        get() = arrayOf(commitTable, snapshotTable)

    companion object {
        val Default: ExposedJaversSchema = ExposedJaversSchema(
            commitTable = CommitTable,
            snapshotTable = CdoSnapshotTable,
        )

        fun from(tableNames: ExposedJaversTableNames): ExposedJaversSchema {
            return if (tableNames == ExposedJaversTableNames.Default) {
                Default
            } else {
                ExposedJaversSchema(
                    commitTable = CommitTableMapping(
                        tableName = tableNames.commitTableName,
                        commitIdIndexName = uniqueIndexName(tableNames.commitTableName, "commit_id"),
                        sequenceIndexName = indexName(tableNames.commitTableName, "sequence"),
                    ),
                    snapshotTable = CdoSnapshotTableMapping(
                        tableName = tableNames.snapshotTableName,
                        globalIdVersionIndexName = uniqueIndexName(tableNames.snapshotTableName, "global_id_version"),
                    ),
                )
            }
        }

        private fun indexName(tableName: String, suffix: String): String {
            return "ix_${tableName}_$suffix"
        }

        private fun uniqueIndexName(tableName: String, suffix: String): String {
            return "ux_${tableName}_$suffix"
        }
    }
}
