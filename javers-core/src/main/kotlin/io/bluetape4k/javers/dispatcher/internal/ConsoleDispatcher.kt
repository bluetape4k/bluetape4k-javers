package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher

/**
 * [JaversDispatcher] implementation that writes domain object change events to standard output.
 */
class ConsoleDispatcher: JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        println("Send saved domain object. $domainObject")
    }

    override fun sendDeleted(domainObject: Any) {
        println("Send deleted domain object. $domainObject")
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        println("Send deleted domain object by id. id=$domainObjectId, type=$domainType")
    }
}
