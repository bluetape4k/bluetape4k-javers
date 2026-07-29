package io.bluetape4k.javers.dispatcher

/**
 * domain object의 save/delete event를 dispatch합니다.
 *
 * ## 계약
 * - [sendSaved]: object가 생성되거나 수정될 때 호출합니다.
 * - [sendDeleted]: object instance가 삭제될 때 호출합니다.
 * - [sendDeletedById]: 삭제가 id로 표현될 때 호출합니다.
 */
interface JaversDispatcher {

    /**
     * domain object save event를 dispatch합니다.
     *
     * @param domainObject 저장된 domain object입니다.
     */
    fun sendSaved(domainObject: Any)

    /**
     * domain object delete event를 dispatch합니다.
     *
     * @param domainObject 삭제된 domain object입니다.
     */
    fun sendDeleted(domainObject: Any)

    /**
     * domain object id로 식별되는 delete event를 dispatch합니다.
     *
     * @param domainObjectId 삭제된 domain object의 id입니다.
     * @param domainType domain object type입니다.
     */
    fun sendDeletedById(domainObjectId: Any, domainType: Class<*>)
}

/**
 * reified domain type을 사용해 id 기반 delete event를 dispatch합니다.
 *
 * ```kotlin
 * dispatcher.sendDeletedById<User>(userId)
 * ```
 */
inline fun <reified T: Any> JaversDispatcher.sendDeletedById(domainObjectId: Any) {
    sendDeletedById(domainObjectId, T::class.java)
}
