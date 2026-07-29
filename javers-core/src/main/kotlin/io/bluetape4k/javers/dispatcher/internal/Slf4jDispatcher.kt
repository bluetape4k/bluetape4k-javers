package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import io.bluetape4k.logging.info

/**
 * domain object change event를 SLF4J [org.slf4j.Logger]에 쓰는 [JaversDispatcher] 구현입니다.
 *
 * @property logger event 기록에 사용하는 SLF4J logger입니다.
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
