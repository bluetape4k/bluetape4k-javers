package io.bluetape4k.javers.persistence.exposed.schema

import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.io.Serializable

/**
 * Exposed JDBC persistence를 위한 encode된 JaVers CDO snapshot을 저장합니다.
 *
 * [state] column은 JaVers가 생성한 전체 encode된 `CdoSnapshot` JSON payload를 담습니다.
 * 다른 column은 indexing, inspection, 향후 SQL pushdown을 위해 query-oriented metadata를 중복 저장합니다.
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
 * JaVers commit metadata와 repository sequence number를 저장합니다.
 *
 * [sequence]는 persistent repository instance를 rebuild한 뒤 최신 `CommitId`를 복원하는 데 사용하는
 * repository-local ordering value입니다.
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
 * [ExposedJaversSchema]가 사용하는 table name입니다.
 *
 * ## 계약
 * 기본값은 기존 `javers_commit`과 `javers_snapshot` table name을 보존합니다.
 * commit/snapshot table ordering 실수를 피하려면 raw string 두 개를 직접 넘기지 말고
 * 이 named value object를 사용하세요.
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
 * `ExposedCdoSnapshotRepository`가 사용하는 Exposed table mapping pair입니다.
 *
 * ## 계약
 * [tables]는 schema creation 및 drop helper에 맞게 정렬되어 있습니다.
 * 기본 schema는 source-compatible [CommitTable]과 [CdoSnapshotTable] singleton object를 사용합니다.
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
