package io.bluetape4k.javers.persistence.exposed.hook

import io.bluetape4k.support.requireNotEmpty
import org.javers.core.Javers
import org.jetbrains.exposed.v1.core.transactions.transactionScope
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.registeredChanges
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.concurrent.atomic.AtomicBoolean

private var isJaversEntityHookAuditing by transactionScope { false }

/**
 * Lifecycle handle for JaVers auditing through Exposed DAO `EntityHook`.
 *
 * ## Contract
 * A subscription registers one global Exposed hook and must be closed when the
 * application scope ends. Only configured DAO entity classes are audited.
 * Created and updated events are committed from the flushed DAO state. Removed
 * events are committed as JaVers terminal snapshots by id.
 *
 * ```kotlin
 * val subscription = ExposedJaversEntityHookSubscription.subscribe(
 *     javers = javers,
 *     mappings = listOf(customerMapping),
 *     authorProvider = { "system" },
 * )
 *
 * subscription.close()
 * ```
 */
class ExposedJaversEntityHookSubscription private constructor(
    private val javers: Javers,
    mappings: List<ExposedJaversEntityHookMapping<*, *, *>>,
    private val authorProvider: (EntityChange) -> String,
    private val commitPropertiesProvider: (EntityChange) -> Map<String, String>,
): AutoCloseable {

    private val mappings: List<ExposedJaversEntityHookMapping<*, *, *>> = mappings.toList()
    private val closed = AtomicBoolean(false)
    private val action: (EntityChange) -> Unit = ::handle

    init {
        this.mappings.requireNotEmpty("mappings")
        EntityHook.subscribe(action)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            EntityHook.unsubscribe(action)
        }
    }

    internal fun handle(change: EntityChange) {
        if (closed.get() || isJaversEntityHookAuditing) {
            return
        }

        val mapping = mappings.firstOrNull { it.matches(change) } ?: return
        if (!change.isLastRegisteredEventForEntity()) {
            return
        }

        withAuditGuard {
            val author = authorProvider(change)
            val properties = commitPropertiesProvider(change)
            when (change.changeType) {
                EntityChangeType.Created,
                EntityChangeType.Updated -> {
                    mapping.toAuditObject(change)?.let { javers.commit(author, it, properties) }
                }
                EntityChangeType.Removed -> {
                    javers.commitShallowDeleteById(author, mapping.toGlobalId(change), properties)
                }
            }
        }
    }

    private fun EntityChange.isLastRegisteredEventForEntity(): Boolean {
        val changes = TransactionManager.current().registeredChanges()
        val last = changes.lastOrNull { it.entityClass == entityClass && it.entityId == entityId }
        return last == this
    }

    private inline fun withAuditGuard(block: () -> Unit) {
        val previous = isJaversEntityHookAuditing
        try {
            isJaversEntityHookAuditing = true
            block()
        } finally {
            isJaversEntityHookAuditing = previous
        }
    }

    companion object {
        /**
         * Registers a JaVers audit hook for the supplied Exposed DAO mappings.
         */
        fun subscribe(
            javers: Javers,
            mappings: List<ExposedJaversEntityHookMapping<*, *, *>>,
            authorProvider: (EntityChange) -> String,
            commitPropertiesProvider: (EntityChange) -> Map<String, String> = { emptyMap() },
        ): ExposedJaversEntityHookSubscription {
            return ExposedJaversEntityHookSubscription(
                javers = javers,
                mappings = mappings,
                authorProvider = authorProvider,
                commitPropertiesProvider = commitPropertiesProvider,
            )
        }
    }
}
