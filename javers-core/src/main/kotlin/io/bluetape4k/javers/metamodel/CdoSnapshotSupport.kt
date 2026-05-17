package io.bluetape4k.javers.metamodel

import io.bluetape4k.javers.isDateInRange
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.repository.api.QueryParams

/**
 * Maps each property of this [CdoSnapshot] through [mapper] and returns the results as a list.
 */
fun <R> CdoSnapshot.mapProperties(mapper: (key: String, value: Any?) -> R): List<R> =
    this.state.mapProperties(mapper)

/**
 * Iterates over each property of this [CdoSnapshot] and invokes [consumer] for each entry.
 */
fun <R> CdoSnapshot.forEachProperties(consumer: (key: String, value: Any?) -> Unit): Unit =
    this.state.forEachProperty(consumer)

/**
 * Filters to snapshots whose commit ID is before or equal to [commitId].
 */
fun Sequence<CdoSnapshot>.filterByToCommitId(commitId: CommitId): Sequence<CdoSnapshot> =
    filter { it.commitId.isBeforeOrEqual(commitId) }

/**
 * Filters to snapshots whose commit ID is contained in [commitIds].
 */
fun Sequence<CdoSnapshot>.filterByCommitIds(commitIds: Collection<CommitId>): Sequence<CdoSnapshot> =
    filter { commitIds.contains(it.commitId) }

/**
 * Filters to snapshots whose version matches [version].
 */
fun Sequence<CdoSnapshot>.filterByVersion(version: Long): Sequence<CdoSnapshot> =
    filter { it.version == version }

/**
 * Filters to snapshots committed by [author].
 */
fun Sequence<CdoSnapshot>.filterByAuthor(author: String): Sequence<CdoSnapshot> =
    filter { it.commitMetadata.author == author }

/**
 * Filters to snapshots whose commit date falls within the from–to range defined by [queryParams].
 */
fun Sequence<CdoSnapshot>.filterByCommitDate(queryParams: QueryParams): Sequence<CdoSnapshot> =
    filter { queryParams.isDateInRange(it.commitMetadata.commitDate) }

/**
 * Filters to snapshots that have a change recorded at [propertyName].
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyName(propertyName: String): Sequence<CdoSnapshot> =
    filter { it.hasChangeAt(propertyName) }

/**
 * Filters to snapshots that have a change in at least one of [propertyNames].
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyNames(propertyNames: Set<String>): Sequence<CdoSnapshot> =
    filter { snapshot ->
        propertyNames.any { propertyName ->
            snapshot.hasChangeAt(propertyName)
        }
    }

/**
 * Filters to snapshots whose type matches [snapshotType].
 */
fun Sequence<CdoSnapshot>.filterByType(snapshotType: SnapshotType): Sequence<CdoSnapshot> =
    filter { it.type == snapshotType }

/**
 * Filters to snapshots whose commit properties satisfy all conditions in [commitProperties].
 */
fun Sequence<CdoSnapshot>.filterByCommitProperties(
    commitProperties: Map<String, Collection<String>>,
): Sequence<CdoSnapshot> =
    filter {
        val props = it.commitMetadata.properties
        commitProperties.all { (key, values) ->
            props.containsKey(key) && values.contains(props[key])
        }
    }

/**
 * Skips [skip] elements and takes up to [limit] elements, returning them as a list.
 */
fun Sequence<CdoSnapshot>.trimToRequestedSlice(skip: Int, limit: Int): List<CdoSnapshot> =
    drop(skip).take(limit).toList()
