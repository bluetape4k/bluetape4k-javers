# Issue 89 Kafka to Redis Projection Plan

## Complexity

Type A feature slice. It touches a new example module, external infrastructure
adapters, integration tests, and user-facing README content.

## Steps

1. Add dependencies to `examples/javers-exposed-ddd`.
   - Kafka clients
   - Lettuce Redis client
   - bluetape4k Testcontainers
   - Kafka Testcontainers

2. Add read-side model and Redis store.
   - `OrderSummary`
   - `RedisOrderSummaryProjection`
   - `OrderQueryService`

3. Add Kafka event path.
   - `OrderDomainEventJsonCodec`
   - `OrderKafkaEventPublisher`
   - `OrderProjectionEventConsumer`

4. Add tests.
   - Codec and Redis projection behavior
   - Kafka producer/consumer to Redis projection integration
   - Existing command handler regression remains passing

5. Update docs.
   - README.md and README.ko.md diagrams and scope text
   - WIP.md active queue
   - `docs/lessons/2026-05-27-issue-89-kafka-redis-projection.md`

6. Review and verify.
   - Local 7-Tier review with P0/P1=0
   - `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `git diff --check`

## Risks

- Kafka/Testcontainers startup can be slow. Tests must stay serial through the
  existing Gradle test mutex and the requested 300 second command timeout.
- Lettuce synchronous commands are safe for this single-test flow, but the
  projection store should keep a dedicated connection and close it.
- JSON event compatibility is example-scoped. It should not become a public
  cross-module serialization contract in this slice.

