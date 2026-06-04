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
 * Filters [Changes] to entries of the requested change type.
 *
 * ```kotlin
 * val valueChanges = changes.filterByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Changes.filterByType(): List<T> =
    this.getChangesByType(T::class.java)

/** Whether this change is an array change. */
val Change.isArrayChange: Boolean get() = this is ArrayChange

/** Whether this change is a list change. */
val Change.isListChange: Boolean get() = this is ListChange

/** Whether this change is a map change. */
val Change.isMapChange: Boolean get() = this is MapChange<*>

/** Whether this change is a set change. */
val Change.isSetChange: Boolean get() = this is SetChange

/** Whether this change is a reference change. */
val Change.isReferenceChange: Boolean get() = this is ReferenceChange

/** Whether this change is a value change. */
val Change.isValueChange: Boolean get() = this is ValueChange

/** Whether this change represents a new object. */
val Change.isNewObject: Boolean get() = this is NewObject

/** Whether this change represents an object removal. */
val Change.isObjectRemoved: Boolean get() = this is ObjectRemoved
