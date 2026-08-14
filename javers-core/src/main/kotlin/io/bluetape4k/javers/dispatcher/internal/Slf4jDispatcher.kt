package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import io.bluetape4k.logging.info

/**
 * 운영 환경에서 domain object change event를 SLF4J [org.slf4j.Logger]에 기록하는
 * [JaversDispatcher] 구현입니다.
 *
 * payload와 identifier는 로그에 남기지 않고 타입 요약만 기록하여 민감한 데이터가
 * 운영 로그로 유출되지 않도록 합니다.
 *
 * @property logger event 기록에 사용하는 SLF4J logger입니다.
 */
class Slf4jDispatcher(private val logger: org.slf4j.Logger): JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        logger.info { "Send saved domain object. type=${safeTypeName(domainObject)}" }
    }

    override fun sendDeleted(domainObject: Any) {
        logger.info { "Send deleted domain object. type=${safeTypeName(domainObject)}" }
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        logger.info { "Send deleted domain object by id. type=${safeTypeName(domainType)}" }
    }

    private fun safeTypeName(domainObject: Any): String =
        domainObject::class.simpleName ?: "<anonymous>"

    private fun safeTypeName(domainType: Class<*>): String =
        domainType.simpleName.takeUnless(String::isBlank) ?: "<anonymous>"
}
