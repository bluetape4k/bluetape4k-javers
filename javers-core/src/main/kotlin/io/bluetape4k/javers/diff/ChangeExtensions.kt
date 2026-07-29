package io.bluetape4k.javers.diff

import org.javers.core.Changes
import org.javers.core.diff.Change
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.ReferenceChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ArrayChange
import org.javers.core.diff.changetype.container.ListChange
import org.javers.core.diff.changetype.container.SetChange
import org.javers.core.diff.changetype.map.MapChange

/**
 * [Changes]에서 요청한 change type의 entry만 filtering합니다.
 *
 * ```kotlin
 * val valueChanges = changes.filterByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Changes.filterByType(): List<T> =
    this.getChangesByType(T::class.java)

/** 이 change가 array change인지 여부입니다. */
val Change.isArrayChange: Boolean get() = this is ArrayChange

/** 이 change가 list change인지 여부입니다. */
val Change.isListChange: Boolean get() = this is ListChange

/** 이 change가 map change인지 여부입니다. */
val Change.isMapChange: Boolean get() = this is MapChange<*>

/** 이 change가 set change인지 여부입니다. */
val Change.isSetChange: Boolean get() = this is SetChange

/** 이 change가 reference change인지 여부입니다. */
val Change.isReferenceChange: Boolean get() = this is ReferenceChange

/** 이 change가 value change인지 여부입니다. */
val Change.isValueChange: Boolean get() = this is ValueChange

/** 이 change가 새 object를 나타내는지 여부입니다. */
val Change.isNewObject: Boolean get() = this is NewObject

/** 이 change가 object 삭제를 나타내는지 여부입니다. */
val Change.isObjectRemoved: Boolean get() = this is ObjectRemoved
