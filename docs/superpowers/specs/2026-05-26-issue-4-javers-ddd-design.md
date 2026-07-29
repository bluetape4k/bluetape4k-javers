# Issue 4 javers-ddd 설계

## 맥락

Issue #4는 `javers-exposed` 이후 Phase 3이다. Aggregate root와 domain event를 위한 JaVers commit/history integration을 쉽게 하는 DDD helper module을 추가한다. 이 module은 하나의 concrete application repository shape에 종속되지 않아야 하며, consumer가 configure한 JaVers instance를 통해 `ExposedCdoSnapshotRepository`와 자연스럽게 동작해야 한다.

## 목표

- `javers-ddd`를 새 published module로 추가한다.
- 작은 public DDD contract를 제공한다.
  - `AggregateRoot<ID : Any>`
  - `DomainEvent`
  - `DomainEvent.toJaversProperties()`
- 다음을 수행하는 abstract `AggregateRepository<T, ID>`를 제공한다.
  - Subclass persistence hook을 통해 aggregate를 save한다.
  - Aggregate를 JaVers에 commit한다.
  - ID로 latest aggregate shadow를 load한다.
  - ID로 JaVers snapshot history를 load한다.
  - Aggregate persistence와 JaVers commit이 성공한 뒤에만 domain event를 publish한다.
- `DomainEventPublisher`와 다음 implementation을 제공한다.
  - no-op/function/composite in core DDD code,
  - Spring `ApplicationEventPublisher`,
  - Spring Kafka `KafkaTemplate`,
  - NATS Java client `Connection`.
- Mermaid class diagram이 포함된 English 및 Korean README file을 추가한다.
- Module을 settings, BOM docs, root README, CI, Nightly에 연결한다.
- 작업 완료 후 `WIP.md`를 갱신한다.

## Non-goal

- 이 issue에서 full outbox table 또는 replay daemon을 추가하지 않는다.
- `javers-ddd`가 concrete Exposed entity repository type에 의존하게 만들지 않는다.
- Phase 4 CQRS/Event Sourcing example을 구현하지 않는다.
- 기존 alias가 있으면 기존 bluetape4k dependency catalog 밖의 새 compatibility-line version을 도입하지 않는다.

## API 결정

### `DomainEvent` Extensibility

GitHub issue는 sealed class를 sketch하지만, sealed public event contract는 consumer가 자신의 module/package에서 event를 선언하지 못하게 한다. 대신 public interface를 사용한다.

```kotlin
interface DomainEvent {
    val aggregateId: Any
    val occurredOn: Instant
    val attributes: Map<String, String>
}
```

`toJaversProperties()`는 stable string commit property를 반환한다.

- `domainEventType`
- `aggregateId`
- `occurredOn`
- `event.<key>` for user attributes

### Aggregate Persistence Boundary

JaVers는 audit/history store이지 aggregate의 source-of-truth table이 아니다. 따라서 `AggregateRepository<T, ID>`는 protected hook을 노출한다.

- `persist(aggregate: T): T`
- `findById(id: ID): T?`

Base class는 JaVers commit/history behavior와 event publication을 처리한다. 이를 통해 Exposed JDBC repository, hand-written Exposed transaction, Spring Data Exposed repository, test in-memory store와 호환된다.

### Event Publisher Boundary

`DomainEventPublisher`는 synchronous 및 fail-fast다. Publisher가 throw하면 repository save call은 aggregate persistence와 JaVers commit이 성공한 뒤 throw한다. After-commit delivery가 필요한 Spring user는 필요할 경우 후속 작업에서 publisher를 Spring transaction synchronization으로 감쌀 수 있다. 이 issue는 full distributed transaction semantics를 주장하지 않고 #4가 요청한 publisher adapter를 제공한다.

## Test 전략

- `DomainEvent.toJaversProperties()`를 unit-test한다.
- No-op/function/composite publisher behavior를 unit-test한다.
- Spring/Kafka/NATS adapter wiring을 mock으로 unit-test한다.
- Save/load/history가 Phase 2 repository를 사용하도록 H2 + `ExposedCdoSnapshotRepository`로 `AggregateRepository`를 integration-test한다.
- Full project를 compile하고 새 module test를 실행한다.
- CI/Nightly edit 후 `actionlint`를 실행한다.

## Risk

- Spring/Kafka/NATS adapter는 optional compile-time surface를 추가한다. 이를 `compileOnly`로 유지하고, adapter 사용 시 consumer가 matching runtime dependency를 추가해야 함을 문서화한다.
- `javers-exposed`는 transitive API dependency가 아니라 이 module의 test/integration dependency로 유지한다. Consumer는 Exposed persistence가 필요할 때 BOM을 통해 두 artifact를 함께 사용할 수 있다.
- JaVers shadow reconstruction은 aggregate class가 stable id를 가진 JaVers-managed entity여야 한다. README example에서 이를 명확히 해야 한다.
- 첫 version은 exactly-once external delivery용 outbox를 대체하지 않는다. Durable outbox가 아니라 immediate publisher adapter로 문서화한다.

## 검토 노트

- Historical external CLI review attempt는 `.omx/artifacts` 아래 기록됐다.
- 결과: `API Error: 400 This organization has been disabled.`로 차단됨.
- Local decision: current issue requirement, source inspection, compile/test validation, local/native 7-tier review를 사용해 구현을 진행한다. 이 historical tool outage는 active process gate가 아니다.
