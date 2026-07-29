package io.bluetape4k.javers.dispatcher.internal

import io.bluetape4k.javers.dispatcher.JaversDispatcher
import java.util.concurrent.CopyOnWriteArrayList

/**
 * verification을 위해 dispatch된 모든 domain object를 보관하는 debug-purpose [CompositeDispatcher]입니다.
 *
 * ## 동작 / 계약
 * - dispatch된 모든 event object를 event category별 [CopyOnWriteArrayList]에 저장합니다.
 * - event 수신 여부는 [isSaved], [isDeleted], [isDeletedById]로 확인합니다.
 * - 저장된 모든 event record를 초기화할 때는 [clear]를 사용합니다.
 *
 * @param dispatchers event를 외부로 전파하는 [JaversDispatcher] instance collection입니다.
 */
class DebugDispatcher(dispatchers: Collection<JaversDispatcher>): CompositeDispatcher(dispatchers) {

    /**
     * delete-by-id event 정보를 보관하는 data class입니다.
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

    /** 지정한 domain object의 save event를 수신했으면 `true`를 반환합니다. */
    fun isSaved(domainObject: Any): Boolean = savedObjects.contains(domainObject)

    /** 지정한 domain object의 delete event를 수신했으면 `true`를 반환합니다. */
    fun isDeleted(domainObject: Any): Boolean = deletedObjects.contains(domainObject)

    /** 지정한 id와 type의 delete-by-id event를 수신했으면 `true`를 반환합니다. */
    fun isDeletedById(id: Any, domainType: Class<*>): Boolean = deletedByIds.contains(DeletedById(id, domainType))

    /** 저장된 모든 event record를 지웁니다. */
    fun clear() {
        savedObjects.clear()
        deletedObjects.clear()
        deletedByIds.clear()
    }
}
