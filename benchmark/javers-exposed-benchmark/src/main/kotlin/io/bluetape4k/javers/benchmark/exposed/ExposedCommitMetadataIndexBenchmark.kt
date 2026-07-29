package io.bluetape4k.javers.benchmark.exposed

import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.jdbc.execCreateMissingTablesAndColumns
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryOptions
import io.bluetape4k.javers.persistence.exposed.schema.CommitTableMapping
import io.bluetape4k.javers.persistence.exposed.schema.ExposedJaversTableNames
import io.bluetape4k.jdbc.hikari.hikariDataSourceOf
import io.bluetape4k.jdbc.sql.withStatement
import io.bluetape4k.support.requireNotNull
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.TypeName
import org.javers.repository.api.QueryParamsBuilder
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.openjdk.jmh.annotations.Level
import java.io.Serializable
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * JaVers Exposed repository의 benchmark 전용 commit metadata index 변형을 측정합니다.
 *
 * 이 benchmark는 production schema를 의도적으로 변경하지 않습니다. 후보 index는
 * trial별 임시 PostgreSQL table에만 생성됩니다.
 */
@State(Scope.Benchmark)
open class ExposedCommitMetadataIndexBenchmark {

    @Param("baseline", "author", "commit_date", "both")
    lateinit var variantName: String

    private lateinit var dataSource: HikariDataSource
    private lateinit var database: Database
    private lateinit var options: ExposedCdoSnapshotRepositoryOptions
    private lateinit var commitTable: CommitTableMapping
    private lateinit var repository: ExposedCdoSnapshotRepository
    private lateinit var javers: Javers
    private lateinit var authors: List<String>
    private lateinit var dateRanges: List<DateRange>

    private val insertCounter = AtomicInteger()
    private val authorCounter = AtomicInteger()
    private val dateRangeCounter = AtomicInteger()

    @Setup(Level.Trial)
    fun setup() {
        val variant = MetadataIndexVariant.from(variantName)
        val tableSuffix = "${variantName}_${Base58.randomString(6)}".replace("-", "_").lowercase()

        dataSource = hikariDataSourceOf(
            jdbcUrl = postgres.getJdbcUrl(),
            username = postgres.getUsername().requireNotNull("postgres.username"),
            password = postgres.getPassword().requireNotNull("postgres.password"),
        ) {
            driverClassName = benchmarkDb.driver
            poolName = "javers-exposed-benchmark-$tableSuffix"
            maximumPoolSize = 4
            minimumIdle = 1
        }
        database = Database.connect(dataSource)
        options = ExposedCdoSnapshotRepositoryOptions(
            tableNames = ExposedJaversTableNames(
                commitTableName = "javers_${tableSuffix}_commit",
                snapshotTableName = "javers_${tableSuffix}_snapshot",
            ),
        )
        val schema = options.newSchema()
        commitTable = schema.commitTable

        transaction(database) {
            execCreateMissingTablesAndColumns(*schema.tables)
            createMetadataIndexes(variant.indexes)
        }

        repository = ExposedCdoSnapshotRepository(database, options = options)
        javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .registerEntity(BenchmarkEntity::class.java)
            .build()
        authors = (0 until AUTHOR_BUCKETS).map { "author-$it" }

        repeat(CORPUS_SIZE) { index ->
            commitSnapshot("corpus", index)
        }
        analyze()
        dateRanges = selectDateRanges()
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::database.isInitialized && ::options.isInitialized) {
            runCatching {
                transaction(database) {
                    SchemaUtils.drop(*options.newSchema().tables)
                }
            }
        }
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }

    @Benchmark
    fun insert(): String {
        val index = insertCounter.getAndIncrement() + CORPUS_SIZE
        return commitSnapshot("insert", index)
    }

    @Benchmark
    fun authorQuery(): Int {
        val author = authors[authorCounter.getAndIncrement() % authors.size]
        return repository
            .getSnapshots(QueryParamsBuilder.withLimit(50).author(author).build())
            .size
    }

    @Benchmark
    fun dateRangeQuery(): Int {
        val range = dateRanges[dateRangeCounter.getAndIncrement() % dateRanges.size]
        return repository
            .getSnapshots(
                QueryParamsBuilder.withLimit(50)
                    .from(range.from)
                    .to(range.to)
                    .build(),
            )
            .size
    }

    private fun createMetadataIndexes(indexes: Set<MetadataIndex>) {
        if (MetadataIndex.Author in indexes) {
            TransactionManager.current()
                .exec("CREATE INDEX ix_${commitTable.tableName}_author ON ${commitTable.tableName} (author)")
        }
        if (MetadataIndex.CommitDate in indexes) {
            TransactionManager.current()
                .exec("CREATE INDEX ix_${commitTable.tableName}_commit_date ON ${commitTable.tableName} (commit_date)")
        }
    }

    private fun commitSnapshot(group: String, index: Int): String {
        val entity = BenchmarkEntity(
            id = "$group-$index",
            value = index,
        )
        return javers.commit("author-${index % AUTHOR_BUCKETS}", entity).id.value()
    }

    private fun analyze() {
        dataSource.withStatement { statement ->
            statement.execute("ANALYZE ${commitTable.tableName}")
        }
    }

    private fun selectDateRanges(): List<DateRange> {
        val commitDates = transaction(database) {
            commitTable
                .selectAll()
                .orderBy(commitTable.sequence, SortOrder.ASC)
                .map { it[commitTable.commitDate] }
        }
        return (0 until DATE_RANGE_BUCKETS).map { index ->
            val start = (index * DATE_RANGE_STEP) % (commitDates.size - DATE_RANGE_WIDTH)
            DateRange(
                from = commitDates[start],
                to = commitDates[start + DATE_RANGE_WIDTH],
            )
        }
    }

    private data class DateRange(
        val from: LocalDateTime,
        val to: LocalDateTime,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private enum class MetadataIndex {
        Author,
        CommitDate,
    }

    private enum class MetadataIndexVariant(
        val paramName: String,
        val indexes: Set<MetadataIndex>,
    ) {
        Baseline("baseline", emptySet()),
        Author("author", setOf(MetadataIndex.Author)),
        CommitDate("commit_date", setOf(MetadataIndex.CommitDate)),
        Both("both", setOf(MetadataIndex.Author, MetadataIndex.CommitDate));

        companion object {
            fun from(paramName: String): MetadataIndexVariant {
                return entries.single { it.paramName == paramName }
            }
        }
    }

    companion object {
        private val postgres by lazy { BenchmarkPostgreSQL.server }
        private val benchmarkDb: TestDB = TestDB.POSTGRESQL
        private const val CORPUS_SIZE = 1_000
        private const val AUTHOR_BUCKETS = 12
        private const val DATE_RANGE_BUCKETS = 128
        private const val DATE_RANGE_WIDTH = 24
        private const val DATE_RANGE_STEP = 3
    }
}

/**
 * commit metadata benchmark corpus에서 사용하는 최소 JaVers entity입니다.
 */
@TypeName("BenchmarkEntity")
data class BenchmarkEntity(
    @Id
    val id: String,
    val value: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
