package io.bluetape4k.javers.persistence.exposed.schema

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * Stores encoded JaVers CDO snapshots for Exposed JDBC persistence.
 *
 * The [state] column contains the full encoded `CdoSnapshot` JSON payload
 * produced by JaVers. Other columns duplicate query-oriented metadata for
 * indexing, inspection, and future SQL pushdown.
 */
object CdoSnapshotTable: LongIdTable("javers_snapshot") {
    const val GLOBAL_ID_VERSION_INDEX = "ux_javers_snapshot_global_id_version"

    val globalId = varchar("global_id", 200).index()
    val commitId = varchar("commit_id", 50).index()
    val version = long("version")
    val type = varchar("type", 50).index()
    val state = text("state")
    val changedProperties = text("changed_properties")
    val managedType = varchar("managed_type", 200).index()

    init {
        uniqueIndex(GLOBAL_ID_VERSION_INDEX, globalId, version)
    }
}

/**
 * Stores JaVers commit metadata and repository sequence numbers.
 *
 * [sequence] is the repository-local ordering value used to restore the latest
 * `CommitId` after rebuilding a persistent repository instance.
 */
object CommitTable: LongIdTable("javers_commit") {
    const val SEQUENCE_INDEX = "ix_javers_commit_sequence"

    val commitId = varchar("commit_id", 50).uniqueIndex()
    val author = varchar("author", 200)
    val commitDate = datetime("commit_date")
    val commitDateInstant = varchar("commit_date_instant", 64).nullable()
    val properties = text("properties")
    val sequence = long("sequence").default(0L).index(SEQUENCE_INDEX)
}
