# Issue 89 Kafka to Redis Projection Design

## Context

Parent issue #5 is split into reviewable slices. PR #91 completed #88 with the
command-side `examples/javers-exposed-ddd` module: Exposed order persistence,
JaVers snapshots, and in-process domain event verification.

Issue #89 adds the query-side slice: publish order domain events through Kafka,
consume them, maintain a Redis-backed read model, and expose a read-side API.

Context7 lookup for Kafka/Lettuce documentation was attempted on 2026-05-27 but
was unavailable because the configured monthly quota was exceeded. Implementation
therefore follows repository-local Kafka, Redis, and Testcontainers patterns.

## Goals

- Add Kafka-backed event publication and consumer handling for order events.
- Add Redis-backed `OrderSummary` projection storage.
- Add a read-side service API for projection lookup.
- Add Testcontainers-backed Kafka and Redis coverage for the projection flow.
- Update English and Korean example README files with command/query diagrams.

## Non-Goals

- Envers benchmark comparison. That remains #90.
- Production-grade outbox, retry, or delivery guarantee implementation.
- Spring Boot HTTP controllers. The example exposes a library-level read API so
  it stays lightweight and focused on the CQRS flow.

## Approach

1. Event codec
   - Encode order domain events as small JSON envelopes with explicit `type`,
     `orderId`, `occurredOn`, and event-specific fields.
   - Decode only known order event types.

2. Kafka adapter
   - Add `OrderKafkaEventPublisher` implementing `DomainEventPublisher`.
   - Add `OrderProjectionEventConsumer` that can process `ConsumerRecord`
     instances and poll a `KafkaConsumer`.

3. Redis projection
   - Add `OrderSummary` read model.
   - Store one JSON document per order under a deterministic Redis key.
   - Update projection idempotently from `OrderPlaced` and `OrderMarkedPaid`.

4. Read API
   - Add `OrderQueryService.findSummary(orderId)`.

## Alternatives Considered

- Spring Kafka listener container: rejected for this slice because the example
  does not need Spring Boot runtime wiring yet.
- Store projection state in Exposed: rejected because #89 explicitly requires
  Redis as the read model.
- Reuse JaVers snapshots for the query side: rejected because the goal is a CQRS
  projection path, not audit-history querying.

## Acceptance Criteria

- Command handler can publish order events to Kafka.
- Consumer can read Kafka records and update Redis projection state.
- `OrderQueryService` returns placed and paid summaries from Redis.
- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` passes.
- README.md, README.ko.md, WIP.md, and a lesson entry are updated.

