package io.bluetape4k.javers.diff

import org.javers.core.diff.Change
import org.javers.core.diff.Diff

/**
 * Returns affected objects for the requested change type from this [Diff].
 *
 * ```kotlin
 * val removedObjects = diff.objectsByChangeType<ObjectRemoved>()
 * // removedObjects == [Employee("To Be Fired")]
 * ```
 */
inline fun <reified T: Change> Diff.objectsByChangeType(): MutableList<Any?> =
    getObjectsByChangeType(T::class.java)

/**
 * Returns changes of the requested type from this [Diff].
 *
 * ```kotlin
 * val valueChanges = diff.changesByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Diff.changesByType(): MutableList<T> =
    getChangesByType(T::class.java)
