package io.bluetape4k.javers.ddd

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.javers.core.Javers
import org.javers.core.commit.Commit

/**
 * 외부 aggregate의 원본 이벤트를 캡처하여 JaVers 감사와 발행 완료를 연결합니다.
 *
 * [eventsOf]는 순서가 고정된 불변 이벤트를 반환하고 [eventMapper]는 원래 ID, 시각,
 * attributes와 eventType을 보존해야 합니다. aggregate와 buffer는 capture부터 완료까지
 * 단일 호출자가 소유하며 상태를 변경하지 않아야 합니다. [clearEvents]는 전체 제거 성공 또는
 * 변경 없는 실패가 원자적이어야 합니다. 임의 객체 내부 변경이나 부분 clear 복구는 지원하지 않습니다.
 *
 * 모든 callback은 호출자 스레드에서 동기 실행합니다. 호출자는 I/O timeout, 실행 스레드,
 * 최대 이벤트 수와 재시도 횟수를 정합니다. 캡처/변환은 O(N)이며 자동 재시도는 없습니다.
 * 새 registration 재시도는 중복 발행할 수 있으므로 publisher/소비자가 멱등성을 소유합니다.
 *
 * ```kotlin
 * val adapter = AggregateAuditAdapter<Order, OrderEvent>(
 *     eventsOf = { it.pendingEvents },
 *     clearEvents = { it.clearEvents() },
 *     eventMapper = { it.toAuditEvent() },
 * )
 * val registration = adapter.capture(order)
 * // source transaction 내부에서 registration.audit(javers, author)를 호출합니다.
 * // transaction의 성공 반환을 확인한 다음에만 publish/complete를 호출합니다.
 * registration.publish { publisher(it) }
 * registration.complete(AuditCompletion.COMMITTED)
 * ```
 */
class AggregateAuditAdapter<A: Any, E: Any>(
    private val eventsOf: (A) -> List<E>,
    private val clearEvents: (A) -> Unit,
    private val eventMapper: (E) -> DomainEvent,
) {
    /** 원본 aggregate와 이벤트 참조 순서를 별도 list에 보관합니다. */
    fun capture(aggregate: A): AggregateAuditRegistration<A, E> = AggregateAuditRegistration(
        aggregate, eventsOf(aggregate).toList(), eventsOf, clearEvents, eventMapper,
    )
}

/** 호출자가 확인한 source transaction의 완료 결과입니다. */
enum class AuditCompletion {
    /** 실제 transaction 성공 반환을 확인했습니다. */
    COMMITTED,
    /** transaction이 rollback되었습니다. */
    ROLLED_BACK,
    /** transaction 결과를 확정할 수 없습니다. */
    UNKNOWN,
}

/**
 * 한 번만 사용하는 감사 등록입니다. audit → transaction 성공 확인 → publish → COMMITTED 순서입니다.
 *
 * ROLLED_BACK/UNKNOWN은 audit 전후 모두 buffer를 보존하고 종료합니다. source와 JaVers backend의
 * transaction 참여는 호출자 설정에 달려 있으며 분산 원자성이나 exactly-once를 보장하지 않습니다.
 * callback 실행 중 재진입, 중복 또는 순서 위반은 IllegalStateException으로 거부합니다.
 * 실패한 등록은 재사용할 수 없으며 callback 예외와 취소는 원래 객체로 전파됩니다.
 */
class AggregateAuditRegistration<A: Any, E: Any> internal constructor(
    private val aggregate: A,
    private val captured: List<E>,
    private val eventsOf: (A) -> List<E>,
    private val clearEvents: (A) -> Unit,
    private val eventMapper: (E) -> DomainEvent,
) {
    private companion object: KLogging()

    private enum class State { OPEN, RUNNING, AUDITED, PUBLISHED, CLOSED, FAILED }
    private var state = State.OPEN

    /** 실패 후에도 조회 가능한 원본 이벤트 참조입니다. 외부 list 변경은 내부 캡처에 영향을 주지 않습니다. */
    val events: List<E> get() = captured.toList()

    /** mapper를 이벤트당 한 번 실행하고 원본 aggregate를 감사합니다. source transaction 안에서 호출합니다. */
    fun audit(javers: Javers, author: String): Commit {
        check(state == State.OPEN) { "Audit requires an open registration" }
        return stage("audit", State.AUDITED) {
            validateBuffer()
            javers.commit(author, aggregate, captured.map(eventMapper).toJaversProperties())
        }
    }

    /** source transaction 성공 확인 후 원본 이벤트를 캡처 순서대로 발행합니다. 부분 실패 시 전체 buffer를 유지합니다. */
    fun publish(publisher: (E) -> Unit) {
        check(state == State.AUDITED) { "Publish requires successful audit" }
        stage("publish", State.PUBLISHED) {
            validateBuffer()
            captured.forEach(publisher)
        }
    }

    /** 감사·전체 발행·COMMITTED가 모두 확인된 경우에만 buffer를 제거합니다. */
    fun complete(completion: AuditCompletion) {
        check(state != State.RUNNING && state != State.CLOSED) { "Registration cannot complete in this state" }
        if (completion != AuditCompletion.COMMITTED) {
            state = State.CLOSED
            return
        }
        check(state == State.PUBLISHED) { "Committed completion requires successful audit and publication" }
        stage("complete", State.CLOSED) {
            validateBuffer()
            clearEvents(aggregate)
        }
    }

    private fun validateBuffer() {
        val current = eventsOf(aggregate)
        check(current.size == captured.size && current.indices.all { current[it] === captured[it] }) {
            "Aggregate event buffer changed after capture"
        }
    }

    private inline fun <T> stage(name: String, success: State, action: () -> T): T {
        state = State.RUNNING
        try {
            val result = action()
            state = success
            return result
        } catch (error: Throwable) {
            state = State.FAILED
            // 고정 단계와 예외 클래스만 기록하여 metadata와 예외 메시지 노출을 방지합니다.
            log.warn { "Aggregate audit stage failed: stage=$name, exception=${error.javaClass.name}" }
            throw error
        }
    }
}
