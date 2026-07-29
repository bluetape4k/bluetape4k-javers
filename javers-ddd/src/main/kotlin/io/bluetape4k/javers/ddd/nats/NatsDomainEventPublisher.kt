package io.bluetape4k.javers.ddd.nats

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.nats.client.Connection

/**
 * NATS Java client [Connection]을 통해 domain event를 publish합니다.
 *
 * ## 계약
 * repository 관점에서 이 publisher는 synchronous합니다. 각 event마다 [Connection.publish]를 호출합니다.
 * payload serialization은 consumer가 제공하므로 이 module은 JSON library나 schema format을 강제하지 않습니다.
 *
 * @property connection NATS connection입니다.
 * @property subjectResolver domain event를 NATS subject로 mapping합니다.
 * @property serializer domain event를 NATS payload로 변환합니다.
 */
class NatsDomainEventPublisher(
    private val connection: Connection,
    private val subjectResolver: (DomainEvent) -> String,
    private val serializer: (DomainEvent) -> ByteArray,
): DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        connection.publish(subjectResolver(event), serializer(event))
    }
}
