package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import io.bluetape4k.logging.info

/**
 * [JaversDispatcher] implementation that writes domain object change events to an SLF4J [org.slf4j.Logger].
 *
 * @property logger SLF4J logger used to record events
 */
class Slf4jDispatcher(private val logger: org.slf4j.Logger): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        logger.info { "Send saved domain object. $domainObject" }
    }

    override fun sendDeleted(domainObject: Any) {
        logger.info { "Send deleted domain object. $domainObject" }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        logger.info { "Send deleted domain object by id. id=$domainObjectId, type=$domainType" }
    }
}
