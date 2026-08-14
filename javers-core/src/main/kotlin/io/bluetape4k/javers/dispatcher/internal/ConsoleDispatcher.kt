package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher

/**
 * 개발·테스트에서 domain object change event를 standard output에 쓰는 [JaversDispatcher] 구현입니다.
 *
 * 운영 wiring에는 사용하지 마십시오. payload와 identifier를 출력하지 않고 안전한 타입 요약만 기록합니다.
 */
class ConsoleDispatcher: JaversDispatcher {

    override fun sendSaved(domainObject: Any) {
        println("Send saved domain object. type=${safeTypeName(domainObject)}")
    }

    override fun sendDeleted(domainObject: Any) {
        println("Send deleted domain object. type=${safeTypeName(domainObject)}")
    }

    override fun sendDeletedById(domainObjectId: Any, domainType: Class<*>) {
        println("Send deleted domain object by id. type=${safeTypeName(domainType)}")
    }

    private fun safeTypeName(domainObject: Any): String =
        domainObject::class.simpleName ?: "<anonymous>"

    private fun safeTypeName(domainType: Class<*>): String =
        domainType.simpleName.takeUnless(String::isBlank) ?: "<anonymous>"
}
