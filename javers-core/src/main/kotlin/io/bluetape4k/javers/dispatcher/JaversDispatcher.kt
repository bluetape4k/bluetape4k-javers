package io.bluetape4k.javers.dispatcher

/**
 * Dispatches save and delete events for domain objects.
 *
 * ## Contract
 * - [sendSaved]: called when an object is created or updated
 * - [sendDeleted]: called when an object instance is deleted
 * - [sendDeletedById]: called when deletion is represented by an id
 */
interface JaversDispatcher {

    /**
     * Dispatches a domain object save event.
     *
     * @param domainObject saved domain object
     */
    fun sendSaved(domainObject: Any)

    /**
     * Dispatches a domain object delete event.
     *
     * @param domainObject deleted domain object
     */
    fun sendDeleted(domainObject: Any)

    /**
     * Dispatches a delete event identified by domain object id.
     *
     * @param domainObjectId id of the deleted domain object
     * @param domainType domain object type
     */
    fun sendDeletedById(domainObjectId: Any, domainType: Class<*>)
}

/**
 * Dispatches an id-based delete event using a reified domain type.
 *
 * ```kotlin
 * dispatcher.sendDeletedById<User>(userId)
 * ```
 */
inline fun <reified T: Any> JaversDispatcher.sendDeletedById(domainObjectId: Any) {
    sendDeletedById(domainObjectId, T::class.java)
}
