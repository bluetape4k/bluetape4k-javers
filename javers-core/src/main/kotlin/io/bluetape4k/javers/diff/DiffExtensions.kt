package io.bluetape4k.javers.diff

import org.javers.core.diff.Change
import org.javers.core.diff.Diff

/**
 * 이 [Diff]에서 요청한 change type의 영향을 받은 object를 반환합니다.
 *
 * ```kotlin
 * val removedObjects = diff.objectsByChangeType<ObjectRemoved>()
 * // removedObjects == [Employee("To Be Fired")]
 * ```
 */
inline fun <reified T: Change> Diff.objectsByChangeType(): MutableList<Any?> =
    getObjectsByChangeType(T::class.java)

/**
 * 이 [Diff]에서 요청한 type의 change를 반환합니다.
 *
 * ```kotlin
 * val valueChanges = diff.changesByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Diff.changesByType(): MutableList<T> =
    getChangesByType(T::class.java)
