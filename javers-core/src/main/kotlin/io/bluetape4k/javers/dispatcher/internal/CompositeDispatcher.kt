package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.collections.forEachCatching
import io.bluetape4k.javers.dispatcher.JaversDispatcher


/**
 * Composite dispatcher that forwards events to multiple [JaversDispatcher] instances in order.
 *
 * ## Contract
 * - Forwards each event to every registered dispatcher.
 * - Ignores exceptions from one dispatcher and continues with the remaining dispatchers.
 *
 * @property dispatchers dispatchers that receive forwarded events
 */
open class CompositeDispatcher(
    val dispatchers: Collection<JaversDispatcher>,
): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendSaved(domainObject)
        }
    }

    override fun sendDeleted(domainObject: Any) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendDeleted(domainObject)
        }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        dispatchers.forEachCatching { dispatcher ->
            dispatcher.sendDeletedById(domainObjectId, domainType)
        }
    }
}
