package io.bluetape4k.javers.persistence.exposed.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.metamodel.filterByAuthor
import io.bluetape4k.javers.metamodel.filterByChangedPropertyNames
import io.bluetape4k.javers.metamodel.filterByCommitDate
import io.bluetape4k.javers.metamodel.filterByCommitIds
import io.bluetape4k.javers.metamodel.filterByCommitProperties
import io.bluetape4k.javers.metamodel.filterByToCommitId
import io.bluetape4k.javers.metamodel.filterByType
import io.bluetape4k.javers.metamodel.filterByVersion
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTableMapping
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTableMapping
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversSchema
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.type.ManagedType
import org.javers.repository.api.QueryParams
import org.javers.repository.api.QueryParamsBuilder
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.Serializable
import kotlin.jvm.optionals.getOrNull

/**
 * [ExposedCdoSnapshotRepository] option입니다.
 *
 * ## 계약
 * 기본값은 기존 `javers_commit` / `javers_snapshot` table mapping을 보존하고
 * [ensureSchema]가 해당 table을 생성하도록 유지합니다.
 * schema migration을 external migration tool이 소유할 때는 [createSchemaOnEnsure]를 `false`로 설정하세요.
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
 * Exposed JDBC 기반 JaVers CDO snapshot repository입니다.
 *
 * ## 계약
 * - [CdoSnapshotTable]에 [CdoSnapshot]마다 한 row를 저장합니다.
 * - [CommitTable]에 JaVers commit마다 한 row를 저장합니다.
 * - [codec]으로 전체 snapshot JSON을 encode하여 JaVers가 구성된 JSON converter로 snapshot을 재구성할 수 있게 합니다.
 * - 모든 operation에 Exposed `transaction {}` 또는 `transaction(database) {}`를 사용하며
 *   JDBC commit을 직접 관리하지 않습니다.
 * - [options]로 table name과 schema creation ownership을 customize합니다.
 *
 * ```kotlin
 * val repository = ExposedCdoSnapshotRepository(database)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repository)
 *     .build()
 * repository.ensureSchema()
 * ```
 *
 * @property database transaction routing에 사용하는 optional Exposed database입니다.
 * @property codec JaVers snapshot JSON persist에 사용하는 codec입니다.
 * @property options table mapping과 schema initialization option입니다.
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

    override fun persistCommit(commit: Commit, sequence: Long) {
        inTransaction {
            super.persistCommit(commit, sequence)
        }
    }

    override fun persistProjectedSnapshot(snapshot: CdoSnapshot, sequence: Long) {
        inTransaction {
            super.persistProjectedSnapshot(snapshot, sequence)
        }
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

    override fun getStateHistory(globalId: GlobalId, queryParams: QueryParams): MutableList<CdoSnapshot> {
        if (queryParams.isAggregate) {
            return super.getStateHistory(globalId, queryParams)
        }
        return applyQueryParams(loadSnapshots(globalId.value()).asSequence(), queryParams)
    }

    override fun getStateHistory(
        givenClasses: MutableSet<ManagedType>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        if (queryParams.isAggregate) {
            return super.getStateHistory(givenClasses, queryParams)
        }

        val managedTypeNames = givenClasses.map { it.name }.toSet()
        if (managedTypeNames.isEmpty()) {
            return mutableListOf()
        }

        val snapshots = inTransaction {
            joinedSnapshotQuery(snapshotTable.managedType inList managedTypeNames)
                .mapNotNull { decode(it[snapshotTable.state]) }
        }
        return applyQueryParams(snapshots.asSequence(), queryParams)
    }

    override fun getSnapshots(queryParams: QueryParams): MutableList<CdoSnapshot> {
        if (!queryParams.canUseSqlPushdown()) {
            return super.getSnapshots(queryParams)
        }

        return inTransaction {
            val query = joinedSnapshotQuery(queryParams.toSqlPredicate())
                .limit(queryParams.limit())
                .offset(queryParams.skip().toLong())
            applyQueryParams(
                snapshots = query.mapNotNull { decode(it[snapshotTable.state]) }.asSequence(),
                queryParams = queryParams.withSqlPageApplied(),
            )
        }
    }

    private fun joinedSnapshotQuery(predicate: Op<Boolean>? = null): Query {
        var query = snapshotTable
            .join(
                otherTable = commitTable,
                joinType = JoinType.INNER,
                additionalConstraint = { snapshotTable.commitId eq commitTable.commitId },
            )
            .selectAll()
            .orderBy(commitTable.sequence, SortOrder.DESC)

        if (predicate != null) {
            query = query.where { predicate }
        }
        return query
    }

    private fun QueryParams.canUseSqlPushdown(): Boolean {
        return toCommitId().isEmpty &&
            authorLikeIgnoreCase().isEmpty &&
            fromInstant().isEmpty &&
            toInstant().isEmpty &&
            commitProperties().isEmpty() &&
            commitPropertiesLike().isEmpty() &&
            changedProperties().isEmpty() &&
            fromVersion().isEmpty &&
            toVersion().isEmpty &&
            !hasSnapshotQueryLimit()
    }

    private fun QueryParams.toSqlPredicate(): Op<Boolean>? {
        val predicates = mutableListOf<Op<Boolean>>()

        val commitIdValues = commitIds().map { it.value() }
        if (commitIdValues.isNotEmpty()) {
            predicates += snapshotTable.commitId inList commitIdValues
        }
        version().getOrNull()?.let { predicates += snapshotTable.version eq it }
        author().getOrNull()?.let { predicates += commitTable.author eq it }
        from().getOrNull()?.let { predicates += commitTable.commitDate greaterEq it }
        to().getOrNull()?.let { predicates += commitTable.commitDate lessEq it }
        snapshotType().getOrNull()?.let { predicates += snapshotTable.type eq it.name }

        return predicates.reduceOrNull { left, right -> left and right }
    }

    private fun QueryParams.withSqlPageApplied(): QueryParams {
        if (skip() == 0) {
            return this
        }
        return QueryParamsBuilder.copy(this)
            .skip(0)
            .build()
    }

    private fun applyQueryParams(
        snapshots: Sequence<CdoSnapshot>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        var results = snapshots
        if (queryParams.commitIds().isNotEmpty()) {
            results = results.filterByCommitIds(queryParams.commitIds())
        }
        if (queryParams.toCommitId().isPresent) {
            results = results.filterByToCommitId(queryParams.toCommitId().get())
        }
        if (queryParams.version().isPresent) {
            results = results.filterByVersion(queryParams.version().get())
        }
        if (queryParams.author().isPresent) {
            results = results.filterByAuthor(queryParams.author().get())
        }
        if (queryParams.from().isPresent || queryParams.to().isPresent) {
            results = results.filterByCommitDate(queryParams)
        }
        if (queryParams.changedProperties().isNotEmpty()) {
            results = results.filterByChangedPropertyNames(queryParams.changedProperties())
        }
        if (queryParams.snapshotType().isPresent) {
            results = results.filterByType(queryParams.snapshotType().get())
        }
        results = results.filterByCommitProperties(queryParams.commitProperties())

        return results
            .drop(queryParams.skip())
            .take(queryParams.limit())
            .toMutableList()
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
