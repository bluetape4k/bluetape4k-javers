package io.bluetape4k.javers.ddd.nats

import io.bluetape4k.javers.ddd.DomainEvent
import io.bluetape4k.javers.ddd.DomainEventPublisher
import io.nats.client.Connection

/**
 * Publishes domain events through a NATS Java client [Connection].
 *
 * ## Contract
 * The publisher is synchronous from the repository point of view: it invokes
 * [Connection.publish] for each event. Payload serialization is supplied by the
 * consumer so this module does not force a JSON library or schema format.
 *
 * @property connection NATS connection
 * @property subjectResolver maps a domain event to a NATS subject
 * @property serializer converts a domain event to a NATS payload
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
