package io.bluetape4k.javers.metamodel

import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType

/**
 * Returns true if this [GlobalId] is the parent of [childCandidate].
 *
 * ## Behavior / Contract
 * - Returns true when this ID is an [InstanceId], [childCandidate] is a [ValueObjectId],
 *   and the child's owner is this ID.
 * - Returns false otherwise.
 */
fun GlobalId.isParent(childCandidate: GlobalId): Boolean {
    if (this !is InstanceId || childCandidate !is ValueObjectId) {
        return false
    }
    return childCandidate.ownerId == this
}

/**
 * Returns true if this [GlobalId] is a child of [parentCandidate].
 *
 * ## Behavior / Contract
 * - Returns true when [parentCandidate] is an [EntityType], this ID is a [ValueObjectId],
 *   and its owner matches the given entity type.
 * - Returns false otherwise.
 */
fun GlobalId.isChild(parentCandidate: ManagedType): Boolean {
    if (parentCandidate !is EntityType || this !is ValueObjectId) {
        return false
    }
    return this.ownerId == parentCandidate
}
