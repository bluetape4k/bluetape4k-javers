# Issue 89 Kafka to Redis Projection Plan

## 복잡도

Type A feature slice. 새 example module, external infrastructure adapter,
integration tests, user-facing README content를 건드린다.

## 단계

1. `examples/javers-exposed-ddd`에 dependency를 추가한다.
   - Kafka clients
   - Lettuce Redis client
   - bluetape4k Testcontainers
   - Kafka Testcontainers

2. read-side model과 Redis store를 추가한다.
   - `OrderSummary`
   - `RedisOrderSummaryProjection`
   - `OrderQueryService`

3. Kafka event path를 추가한다.
   - `OrderDomainEventJsonCodec`
   - `OrderKafkaEventPublisher`
   - `OrderProjectionEventConsumer`

4. tests를 추가한다.
   - Codec 및 Redis projection behavior
   - Kafka producer/consumer to Redis projection integration
   - 기존 command handler regression은 계속 통과해야 한다.

5. docs를 갱신한다.
   - README.md and README.ko.md diagrams and scope text
   - WIP.md active queue
   - `docs/lessons/2026-05-27-issue-89-kafka-redis-projection.md`

6. review 및 검증을 수행한다.
   - Local 7-Tier review with P0/P1=0
   - `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `git diff --check`

## 위험

- Kafka/Testcontainers startup은 느릴 수 있다. tests는 기존 Gradle test mutex와 요청된 300초 command timeout 안에서 serial 상태를 유지해야 한다.
- Lettuce synchronous command는 이 single-test flow에서는 안전하지만 projection store는 dedicated connection을 유지하고 닫아야 한다.
- JSON event compatibility는 example-scoped다. 이 slice에서 public cross-module serialization contract가 되면 안 된다.
