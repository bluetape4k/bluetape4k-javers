package io.bluetape4k.javers.persistence.exposed.hook

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.persistence.exposed.schema.CdoSnapshotTable
import io.bluetape4k.javers.persistence.exposed.schema.CommitTable
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.repository.jql.QueryBuilder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class ExposedJaversEntityHookSubscriptionH2Test {

    private val database: Database = Database.connect(
        url = "jdbc:h2:mem:javers-entityhook-${Base58.randomString(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )

    @BeforeEach
    fun beforeEach() {
        transaction(database) {
            SchemaUtils.drop(AuditCustomers, CdoSnapshotTable, CommitTable)
            SchemaUtils.create(CommitTable, CdoSnapshotTable, AuditCustomers)
        }
    }

    @Test
    fun `created entity is audited from Exposed DAO hook`() {
        val javers = newJavers()

        val id = withSubscription(javers) {
            transaction(database) {
                AuditCustomerEntity.new {
                    name = "Alice"
                    score = 10
                }.id.value
            }
        }

        val snapshots = snapshots(javers, id)

        snapshots shouldHaveSize 1
        snapshots.single().type shouldBeEqualTo SnapshotType.INITIAL
        snapshots.single().getPropertyValue("name") shouldBeEqualTo "Alice"
        snapshots.single().getPropertyValue("score") shouldBeEqualTo 10
        snapshots.single().commitMetadata.properties["changeType"] shouldBeEqualTo "Created"
    }

    @Test
    fun `updated entity is audited with final flushed state`() {
        val javers = newJavers()

        val id = withSubscription(javers) {
            val createdId = transaction(database) {
                AuditCustomerEntity.new {
                    name = "Bob"
                    score = 10
                }.id.value
            }

            transaction(database) {
                val customer = AuditCustomerEntity.findById(createdId).shouldNotBeNull()
                customer.name = "Bobby"
                customer.score = 20
            }
            createdId
        }

        val snapshots = snapshots(javers, id)

        snapshots shouldHaveSize 2
        snapshots[0].type shouldBeEqualTo SnapshotType.UPDATE
        snapshots[0].getPropertyValue("name") shouldBeEqualTo "Bobby"
        snapshots[0].getPropertyValue("score") shouldBeEqualTo 20
        snapshots[1].type shouldBeEqualTo SnapshotType.INITIAL
    }

    @Test
    fun `removed entity is audited as terminal snapshot by id`() {
        val javers = newJavers()

        val id = withSubscription(javers) {
            val createdId = transaction(database) {
                AuditCustomerEntity.new {
                    name = "Carol"
                    score = 30
                }.id.value
            }

            transaction(database) {
                val customer = AuditCustomerEntity.findById(createdId).shouldNotBeNull()
                customer.delete()
            }
            createdId
        }

        val snapshots = snapshots(javers, id)

        snapshots shouldHaveSize 2
        snapshots[0].type shouldBeEqualTo SnapshotType.TERMINAL
        snapshots[1].type shouldBeEqualTo SnapshotType.INITIAL
    }

    @Test
    fun `multiple updates in one transaction create one final snapshot`() {
        val javers = newJavers()

        val id = withSubscription(javers) {
            val createdId = transaction(database) {
                AuditCustomerEntity.new {
                    name = "Dave"
                    score = 10
                }.id.value
            }

            transaction(database) {
                val customer = AuditCustomerEntity.findById(createdId).shouldNotBeNull()
                customer.score = 20
                customer.score = 30
                customer.name = "David"
            }
            createdId
        }

        val snapshots = snapshots(javers, id)

        snapshots shouldHaveSize 2
        snapshots[0].getPropertyValue("name") shouldBeEqualTo "David"
        snapshots[0].getPropertyValue("score") shouldBeEqualTo 30
    }

    @Test
    fun `rolled back source transaction rolls back JaVers snapshot rows`() {
        val javers = newJavers()
        var id = 0

        withSubscription(javers) {
            assertFailsWith<IllegalStateException> {
                transaction(database) {
                    val customer = AuditCustomerEntity.new {
                        name = "Erin"
                        score = 40
                    }
                    id = customer.id.value
                    AuditCustomers.selectAll().count()

                    throw IllegalStateException("rollback")
                }
            }
        }

        snapshots(javers, id) shouldHaveSize 0
        transaction(database) {
            CdoSnapshotTable.selectAll().count().shouldBeZero()
            CommitTable.selectAll().count().shouldBeZero()
        }
    }

    @Test
    fun `close unsubscribes global entity hook action`() {
        val javers = newJavers()
        val subscription = newSubscription(javers)
        subscription.close()

        val id = transaction(database) {
            AuditCustomerEntity.new {
                name = "Frank"
                score = 50
            }.id.value
        }

        snapshots(javers, id) shouldHaveSize 0
    }

    @Test
    fun `reentrancy guard skips nested hook callback`() {
        val commitCalls = AtomicInteger()
        var activeChange: EntityChange? = null
        lateinit var subscription: ExposedJaversEntityHookSubscription
        val javers = recursiveJavers {
            commitCalls.incrementAndGet()
            val change = activeChange.shouldNotBeNull()
            subscription.handle(change)
        }

        subscription = ExposedJaversEntityHookSubscription.subscribe(
            javers = javers,
            mappings = listOf(newMapping()),
            authorProvider = { change ->
                activeChange = change
                "entity-hook"
            },
        )

        try {
            transaction(database) {
                AuditCustomerEntity.new {
                    name = "Grace"
                    score = 60
                }.id.value
            }
        } finally {
            subscription.close()
        }

        commitCalls.get() shouldBeEqualTo 1
    }

    private fun newJavers(): Javers {
        val repository = ExposedCdoSnapshotRepository(database)
        return JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()
    }

    private fun newSubscription(javers: Javers): ExposedJaversEntityHookSubscription {
        val mapping = newMapping()

        return ExposedJaversEntityHookSubscription.subscribe(
            javers = javers,
            mappings = listOf(mapping),
            authorProvider = { "entity-hook" },
            commitPropertiesProvider = { change -> mapOf("changeType" to change.changeType.name) },
        )
    }

    private fun newMapping(): ExposedJaversEntityHookMapping<Int, AuditCustomerEntity, AuditedCustomer> {
        return ExposedJaversEntityHookMapping(
            entityClass = AuditCustomerEntity,
            auditType = AuditedCustomer::class.java,
            toAuditObject = { entity ->
                AuditedCustomer(
                    id = entity.id.value,
                    name = entity.name,
                    score = entity.score,
                )
            },
        )
    }

    private fun recursiveJavers(onCommit: () -> Unit): Javers {
        return Proxy.newProxyInstance(
            Javers::class.java.classLoader,
            arrayOf(Javers::class.java),
        ) { _, method, _ ->
            if (method.name == "commit" && method.parameterCount == 3) {
                onCommit()
            }
            defaultReturnValue(method.returnType)
        } as Javers
    }

    private fun defaultReturnValue(returnType: Class<*>): Any? {
        return when (returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0F
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            else -> null
        }
    }

    private fun <T> withSubscription(javers: Javers, block: () -> T): T {
        val subscription = newSubscription(javers)
        try {
            return block()
        } finally {
            subscription.close()
        }
    }

    private fun snapshots(javers: Javers, id: Int) =
        javers.findSnapshots(QueryBuilder.byInstanceId(id, AuditedCustomer::class.java).build())

    private object AuditCustomers: IntIdTable("audit_customers") {
        val name = varchar("name", 100)
        val score = integer("score")
    }

    private class AuditCustomerEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<AuditCustomerEntity>(
            table = AuditCustomers,
            entityType = AuditCustomerEntity::class.java,
            entityCtor = ::AuditCustomerEntity,
        )

        var name by AuditCustomers.name
        var score by AuditCustomers.score
    }

    private data class AuditedCustomer(
        @Id val id: Int,
        val name: String,
        val score: Int,
    ): Serializable {

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
