# Issue 89 Kafka to Redis Projection 설계

## 맥락

Parent issue #5는 review 가능한 slice로 나뉜다. PR #91은 command-side
`examples/javers-exposed-ddd` module로 #88을 완료했다. 해당 slice는 Exposed order
persistence, JaVers snapshot, in-process domain event verification을 포함한다.

Issue #89는 query-side slice를 추가한다. Order domain event를 Kafka로 publish하고,
이를 consume하며, Redis-backed read model을 유지하고, read-side API를 노출한다.

Kafka/Lettuce documentation에 대한 Context7 lookup을 2026-05-27에 시도했지만,
configured monthly quota 초과로 사용할 수 없었다. 따라서 구현은 repository-local Kafka,
Redis, Testcontainers pattern을 따른다.

## 목표

- Order event를 위한 Kafka-backed event publication 및 consumer handling을 추가한다.
- Redis-backed `OrderSummary` projection storage를 추가한다.
- Projection lookup을 위한 read-side service API를 추가한다.
- Projection flow에 대한 Testcontainers-backed Kafka 및 Redis coverage를 추가한다.
- Command/query diagram을 포함하도록 English 및 Korean example README file을 갱신한다.

## Non-goal

- Envers benchmark comparison. 이는 #90에 남긴다.
- Production-grade outbox, retry, delivery guarantee 구현.
- Spring Boot HTTP controller. Example은 library-level read API를 노출하여 가볍게 유지하고 CQRS flow에 집중한다.

## 접근

1. Event codec
   - Order domain event를 명시적 `type`, `orderId`, `occurredOn`, event-specific field가 있는 작은 JSON envelope로 encode한다.
   - 알려진 order event type만 decode한다.

2. Kafka adapter
   - `DomainEventPublisher`를 구현하는 `OrderKafkaEventPublisher`를 추가한다.
   - `ConsumerRecord` instance를 처리하고 `KafkaConsumer`를 poll할 수 있는 `OrderProjectionEventConsumer`를 추가한다.

3. Redis projection
   - `OrderSummary` read model을 추가한다.
   - Deterministic Redis key 아래 order별 JSON document 하나를 저장한다.
   - `OrderPlaced`와 `OrderMarkedPaid`에서 projection을 idempotent하게 갱신한다.

4. Read API
   - `OrderQueryService.findSummary(orderId)`를 추가한다.

## 검토한 대안

- Spring Kafka listener container: example이 아직 Spring Boot runtime wiring을 필요로 하지 않으므로 이 slice에서는 기각한다.
- Projection state를 Exposed에 저장: #89가 read model로 Redis를 명시적으로 요구하므로 기각한다.
- Query side에서 JaVers snapshot 재사용: 목표가 audit-history query가 아니라 CQRS projection path이므로 기각한다.

## 인수 기준

- Command handler가 order event를 Kafka에 publish할 수 있다.
- Consumer가 Kafka record를 읽고 Redis projection state를 갱신할 수 있다.
- `OrderQueryService`가 Redis에서 placed 및 paid summary를 반환한다.
- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`이 통과한다.
- README.md, README.ko.md, WIP.md, lesson entry가 갱신된다.
