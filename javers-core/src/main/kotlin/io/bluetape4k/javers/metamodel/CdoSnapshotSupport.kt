package io.bluetape4k.javers.metamodel

import io.bluetape4k.javers.isDateInRange
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.repository.api.QueryParams

/**
 * 이 [CdoSnapshot]의 각 property를 [mapper]로 변환하고 결과를 list로 반환합니다.
 */
fun <R> CdoSnapshot.mapProperties(mapper: (key: String, value: Any?) -> R): List<R> =
    this.state.mapProperties(mapper)

/**
 * 이 [CdoSnapshot]의 각 property를 순회하며 각 entry마다 [consumer]를 호출합니다.
 */
fun <R> CdoSnapshot.forEachProperties(consumer: (key: String, value: Any?) -> Unit): Unit =
    this.state.forEachProperty(consumer)

/**
 * commit ID가 [commitId]보다 이전이거나 같은 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByToCommitId(commitId: CommitId): Sequence<CdoSnapshot> =
    filter { it.commitId.isBeforeOrEqual(commitId) }

/**
 * commit ID가 [commitIds]에 포함된 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByCommitIds(commitIds: Collection<CommitId>): Sequence<CdoSnapshot> =
    filter { commitIds.contains(it.commitId) }

/**
 * version이 [version]과 일치하는 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByVersion(version: Long): Sequence<CdoSnapshot> =
    filter { it.version == version }

/**
 * [author]가 commit한 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByAuthor(author: String): Sequence<CdoSnapshot> =
    filter { it.commitMetadata.author == author }

/**
 * commit date가 [queryParams]에 정의된 from-to 범위 안에 있는 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByCommitDate(queryParams: QueryParams): Sequence<CdoSnapshot> =
    filter { queryParams.isDateInRange(it.commitMetadata.commitDate) }

/**
 * [propertyName]에 기록된 change가 있는 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyName(propertyName: String): Sequence<CdoSnapshot> =
    filter { it.hasChangeAt(propertyName) }

/**
 * [propertyNames] 중 하나 이상에 change가 있는 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyNames(propertyNames: Set<String>): Sequence<CdoSnapshot> =
    filter { snapshot ->
        propertyNames.any { propertyName ->
            snapshot.hasChangeAt(propertyName)
        }
    }

/**
 * type이 [snapshotType]과 일치하는 snapshot만 남깁니다.
 */
fun Sequence<CdoSnapshot>.filterByType(snapshotType: SnapshotType): Sequence<CdoSnapshot> =
    filter { it.type == snapshotType }

/**
 * commit properties가 [commitProperties]의 모든 조건을 만족하는 snapshot만 남깁니다.
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
 * [skip]개 element를 건너뛰고 최대 [limit]개 element를 list로 반환합니다.
 */
fun Sequence<CdoSnapshot>.trimToRequestedSlice(skip: Int, limit: Int): List<CdoSnapshot> =
    drop(skip).take(limit).toList()
