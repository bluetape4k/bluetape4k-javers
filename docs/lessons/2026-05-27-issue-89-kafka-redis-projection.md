# Issue 89 Kafka to Redis Projection Lesson

## Context

#89 follows the merged #88 command-side example and adds the query-side CQRS
projection for parent #5.

## Decision

Keep the example runtime lightweight: plain Kafka clients publish/consume JSON
order event envelopes, and Lettuce stores one Redis JSON document per
`OrderSummary`. Expose the read side as `OrderQueryService` instead of adding
Spring Boot HTTP wiring in this slice.

## Outcome

The command handler can publish `OrderPlaced` and `OrderMarkedPaid` events to
Kafka, the projection consumer updates Redis, and the read API returns the
placed/paid summary.

## Verification

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passed with 3 tests, including Kafka and Redis Testcontainers flow.

## Future Guidance

Keep #90 focused on fresh benchmark evidence. Do not fold Envers comparison or
production outbox guarantees back into this projection slice.
