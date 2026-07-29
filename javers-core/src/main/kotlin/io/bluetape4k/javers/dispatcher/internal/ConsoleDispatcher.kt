package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher

/**
 * domain object change event를 standard output에 쓰는 [JaversDispatcher] 구현입니다.
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
