package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A debug-purpose [CompositeDispatcher] that retains all dispatched domain objects for verification.
 *
 * ## Behavior / Contract
 * - All dispatched event objects are stored in a [CopyOnWriteArrayList] per event category.
 * - Use [isSaved], [isDeleted], and [isDeletedById] to verify whether events were received.
 * - Use [clear] to reset all stored event records.
 *
 * @param dispatchers collection of [JaversDispatcher] instances that propagate events externally
 */
class DebugDispatcher(dispatchers: Collection<JaversDispatcher>): CompositeDispatcher(dispatchers) {

    /**
     * Data class holding information about a delete-by-id event.
     */
    data class DeletedById(val id: Any, val domainType: Class<*>)

    private val savedObjects = CopyOnWriteArrayList<Any>()
    private val deletedObjects = CopyOnWriteArrayList<Any>()
    private val deletedByIds = CopyOnWriteArrayList<DeletedById>()

    override fun sendSaved(domainObject: Any) {
        savedObjects.add(domainObject)
        super.sendSaved(domainObject)
    }

    override fun sendDeleted(domainObject: Any) {
        deletedObjects.add(domainObject)
        super.sendDeleted(domainObject)
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        deletedByIds.add(DeletedById(domainObjectId, domainType))
        super.sendDeletedById(domainObjectId, domainType)
    }

    /** Returns true if a save event was received for the given domain object. */
    fun isSaved(domainObject: Any): Boolean = savedObjects.contains(domainObject)

    /** Returns true if a delete event was received for the given domain object. */
    fun isDeleted(domainObject: Any): Boolean = deletedObjects.contains(domainObject)

    /** Returns true if a delete-by-id event was received for the given id and type. */
    fun isDeletedById(id: Any, domainType: Class<*>): Boolean = deletedByIds.contains(DeletedById(id, domainType))

    /** Clears all stored event records. */
    fun clear() {
        savedObjects.clear()
        deletedObjects.clear()
        deletedByIds.clear()
    }
}
