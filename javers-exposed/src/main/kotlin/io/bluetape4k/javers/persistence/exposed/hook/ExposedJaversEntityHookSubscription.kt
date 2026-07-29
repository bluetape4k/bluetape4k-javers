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
 * Exposed DAO `EntityHook`을 통한 JaVers auditing lifecycle handle입니다.
 *
 * ## 계약
 * subscription은 global Exposed hook 하나를 등록하며 application scope가 끝날 때 close되어야 합니다.
 * 구성된 DAO entity class만 audit합니다.
 * Created/Updated event는 flushed DAO state에서 commit합니다.
 * Removed event는 id 기반 JaVers terminal snapshot으로 commit합니다.
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
         * 제공된 Exposed DAO mapping에 대해 JaVers audit hook을 등록합니다.
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
