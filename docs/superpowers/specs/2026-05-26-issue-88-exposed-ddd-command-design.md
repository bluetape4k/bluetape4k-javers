# Issue #88 — javers-exposed-ddd Command-Side Example 설계

일자: 2026-05-26
Issue: https://github.com/bluetape4k/bluetape4k-javers/issues/88
Parent: https://github.com/bluetape4k/bluetape4k-javers/issues/5

## 맥락

Issue #5는 JaVers, Exposed, Kotlin, Kafka, Redis를 결합한 CQRS / Event Sourcing example을 요구한다. 이 scope는 review 가능한 PR 하나로 다루기에는 너무 크므로 command-side, projection-side, benchmark/document work로 나눈다. Issue #88은 첫 slice다.

새로 merge된 `javers-ddd` module은 다음을 제공한다.

- `AggregateRoot<ID>`
- `DomainEvent`
- `AggregateRepository<T, ID>`
- `DomainEventPublisher`

새로 merge된 `javers-exposed` module은 다음을 제공한다.

- `ExposedCdoSnapshotRepository`
- JaVers snapshot and commit tables for Exposed JDBC persistence

## 목표

Command-side aggregate persistence를 보여주는 example module을 추가한다.

```text
OrderCommandHandler
  -> Exposed order table
  -> AggregateRepository.save(...)
  -> JaVers commit through ExposedCdoSnapshotRepository
  -> DomainEventPublisher
```

Example은 독립적으로 review할 수 있을 만큼 작아야 하며 Kafka consumer, Redis projection, benchmark를 포함하면 안 된다.

## 범위

- `javers-exposed-ddd`를 Gradle example module로 추가한다.
- ID value type, command type, line item, status transition, domain event를 가진 order aggregate를 model한다.
- Exposed JDBC table로 order state를 persist한다.
- Order repository를 `AggregateRepository`와 integrate한다.
- Place/mark-paid command를 처리하는 `OrderCommandHandler`를 추가한다.
- 다음을 증명하는 H2-backed test를 추가한다.
  - Command handler가 aggregate state를 persist한다.
  - JaVers가 snapshot과 commit property를 저장한다.
  - Domain event가 save 후 publish된다.
  - Repository load가 source table에서 state를 reconstruct할 수 있다.
- Example이 계속 실행되도록 top-level docs, WIP, CI, Nightly를 갱신한다.

## 제외 목표

- Kafka event consumption 및 Redis read model projection. #89가 다룬다.
- Envers comparison benchmark. #90이 다룬다.
- Production outbox semantics. Example은 `javers-ddd`의 immediate publisher helper를 사용한다.
- Spring Boot auto-configuration. 첫 slice는 framework wiring을 작게 유지하고 command-side domain flow에 집중한다.

## API 형태

Example module은 다음 아래 public class를 사용한다.

```text
io.bluetape4k.javers.examples.exposedddd
```

계획된 type:

- `OrderId`
- `CustomerId`
- `OrderItem`
- `OrderStatus`
- `Order`
- `OrderCommand`
- `OrderPlaced`
- `OrderMarkedPaid`
- `OrderRepository`
- `OrderCommandHandler`

`Order`는 `AggregateRoot<OrderId>`를 구현하고 JaVers ID property를 `@Id`로 표시한다.

## Persistence 형태

Command-side source table은 의도적으로 단순하다.

- `example_order`
  - `id`
  - `customer_id`
  - `status`
  - `items_json`
  - `created_at`
  - `updated_at`

이 slice에서는 line item을 JSON으로 serialize한다. Normalized order item table은 #88이 증명하려는 JaVers/DDD integration에서 초점을 흐린다.

## 검증

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## 위험

- JaVers는 Kotlin inline/value ID와 collection을 엄격하게 다룬다. Test는 mapped aggregate가 value object가 아니라 entity임을 검증해야 한다.
- Example dependency는 published library API를 확장하면 안 된다. Example module은 `javers-ddd`와 `javers-exposed`에 implementation dependency를 사용할 수 있다.
- CI path filter가 example path를 포함해야 한다. 그렇지 않으면 module test가 조용히 skip될 수 있다.

## 검토 노트

이전 #4 외부 CLI 검토 시도는 `API Error: 400 This organization has been disabled.`를 반환했다. 현재 workflow policy는 local/native 7-tier review와 CI evidence를 필수 gate로 사용하므로, 해당 과거 도구 장애는 현재 blocker가 아니다.
