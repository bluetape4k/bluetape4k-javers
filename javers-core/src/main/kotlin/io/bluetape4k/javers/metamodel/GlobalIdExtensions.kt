package io.bluetape4k.javers.metamodel

import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType

/**
 * 이 [GlobalId]가 [childCandidate]의 parent이면 `true`를 반환합니다.
 *
 * ## 동작 / 계약
 * - 이 ID가 [InstanceId]이고 [childCandidate]가 [ValueObjectId]이며,
 *   child의 owner가 이 ID이면 `true`를 반환합니다.
 * - 그 외에는 `false`를 반환합니다.
 */
fun GlobalId.isParent(childCandidate: GlobalId): Boolean {
    if (this !is InstanceId || childCandidate !is ValueObjectId) {
        return false
    }
    return childCandidate.ownerId == this
}

/**
 * 이 [GlobalId]가 [parentCandidate]의 child이면 `true`를 반환합니다.
 *
 * ## 동작 / 계약
 * - [parentCandidate]가 [EntityType]이고 이 ID가 [ValueObjectId]이며,
 *   owner가 주어진 entity type과 일치하면 `true`를 반환합니다.
 * - 그 외에는 `false`를 반환합니다.
 */
fun GlobalId.isChild(parentCandidate: ManagedType): Boolean {
    if (parentCandidate !is EntityType || this !is ValueObjectId) {
        return false
    }
    return this.ownerId == parentCandidate
}
