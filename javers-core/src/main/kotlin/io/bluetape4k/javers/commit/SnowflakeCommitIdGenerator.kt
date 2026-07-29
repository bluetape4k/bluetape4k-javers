package io.bluetape4k.javers.commit

import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import org.javers.core.commit.CommitId
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.withLock

/**
 * [Snowflake] algorithm을 기반으로 하는 [CommitId] generator입니다.
 *
 * ## 동작 / 계약
 * - 각 [get] 호출은 Snowflake ID를 majorId로, minorId=0으로 갖는 고유 [CommitId]를 생성합니다.
 * - 생성된 각 [CommitId]에 배정된 sequence number는 [getSeq]로 조회할 수 있습니다.
 * - 이 instance가 생성하지 않은 [CommitId]로 [getSeq]를 호출하면 [NoSuchElementException]을 던집니다.
 * - [get]은 내부 lock을 통해 thread-safe합니다.
 *
 * ```kotlin
 * val generator = SnowflakeCommitIdGenerator()
 * val commitId = generator.get()
 * val seq = generator.getSeq(commitId)
 * // seq == 1
 * ```
 *
 * @property snowflake ID 생성에 사용하는 [Snowflake] instance입니다.
 */
class SnowflakeCommitIdGenerator(
    private val snowflake: Snowflake = Snowflakers.Default,
): Supplier<CommitId> {

    private val commits = ConcurrentHashMap<CommitId, Int>()
    private val counter = atomic(0)
    private val lock = reentrantLock()

    /**
     * [commitId]에 배정된 sequence number를 반환합니다.
     *
     * @throws NoSuchElementException [commitId]가 이 instance에서 생성되지 않았을 때 발생합니다.
     */
    fun getSeq(commitId: CommitId): Int =
        commits[commitId] ?: throw NoSuchElementException("Not found commitId [$commitId]")

    override fun get(): CommitId = lock.withLock {
        counter.incrementAndGet()
        val next = CommitId(nextId(), 0)
        commits[next] = counter.value
        next
    }

    private fun nextId(): Long = snowflake.nextId()
}
