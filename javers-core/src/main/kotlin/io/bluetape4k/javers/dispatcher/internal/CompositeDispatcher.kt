package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.collections.forEachCatching
import io.bluetape4k.javers.dispatcher.JaversDispatcher


/**
 * 여러 [JaversDispatcher] instance로 event를 순서대로 전달하는 composite dispatcher입니다.
 *
 * ## 계약
 * - 각 event를 등록된 모든 dispatcher로 전달합니다.
 * - 한 dispatcher에서 발생한 exception은 무시하고 나머지 dispatcher 처리를 계속합니다.
 *
 * @property dispatchers 전달된 event를 수신하는 dispatcher 목록입니다.
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
