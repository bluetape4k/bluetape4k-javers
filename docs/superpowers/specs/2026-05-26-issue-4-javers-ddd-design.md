# Issue 4 javers-ddd Design

## Context

Issue #4 is Phase 3 after `javers-exposed`: add a DDD helper module that makes
JaVers commit/history integration easier for aggregate roots and domain events.
The module must remain independent from one concrete application repository
shape while still working naturally with `ExposedCdoSnapshotRepository` through a
JaVers instance configured by the consumer.

## Goals

- Add `javers-ddd` as a new published module.
- Provide small public DDD contracts:
  - `AggregateRoot<ID : Any>`
  - `DomainEvent`
  - `DomainEvent.toJaversProperties()`
- Provide an abstract `AggregateRepository<T, ID>` that:
  - saves the aggregate through subclass persistence hooks,
  - commits the aggregate to JaVers,
  - loads the latest aggregate shadow by id,
  - loads JaVers snapshot history by id,
  - publishes domain events only after the aggregate persistence and JaVers
    commit succeed.
- Provide `DomainEventPublisher` plus implementations for:
  - no-op/function/composite in core DDD code,
  - Spring `ApplicationEventPublisher`,
  - Spring Kafka `KafkaTemplate`,
  - NATS Java client `Connection`.
- Add English and Korean README files with Mermaid class diagrams.
- Wire the module into settings, BOM docs, root README, CI, and Nightly.
- Update `WIP.md` after completing the work.

## Non-Goals

- Do not add a full outbox table or replay daemon in this issue.
- Do not make `javers-ddd` depend on a concrete Exposed entity repository type.
- Do not implement the Phase 4 CQRS/Event Sourcing example.
- Do not introduce new compatibility-line versions outside the existing
  bluetape4k dependency catalog when an existing alias is available.

## API Decisions

### `DomainEvent` Extensibility

The GitHub issue sketches a sealed class, but a sealed public event contract
would prevent consumers from declaring events in their own modules/packages.
Use a public interface instead:

```kotlin
interface DomainEvent {
    val aggregateId: Any
    val occurredOn: Instant
    val attributes: Map<String, String>
}
```

`toJaversProperties()` returns stable string commit properties:

- `domainEventType`
- `aggregateId`
- `occurredOn`
- `event.<key>` for user attributes

### Aggregate Persistence Boundary

JaVers is the audit/history store, not the aggregate's source-of-truth table.
`AggregateRepository<T, ID>` therefore exposes protected hooks:

- `persist(aggregate: T): T`
- `findById(id: ID): T?`

The base class handles JaVers commit/history behavior and event publication.
This keeps it compatible with Exposed JDBC repositories, hand-written Exposed
transactions, Spring Data Exposed repositories, or test in-memory stores.

### Event Publisher Boundary

`DomainEventPublisher` is synchronous and fail-fast. If a publisher throws, the
repository save call throws after aggregate persistence and JaVers commit have
succeeded. Spring users who need after-commit delivery can wrap the publisher
with Spring transaction synchronization in a follow-up if needed; this issue
ships the publisher adapters requested by #4 without claiming full distributed
transaction semantics.

## Testing Strategy

- Unit-test `DomainEvent.toJaversProperties()`.
- Unit-test no-op/function/composite publisher behavior.
- Unit-test Spring/Kafka/NATS adapter wiring with mocks.
- Integration-test `AggregateRepository` with H2 + `ExposedCdoSnapshotRepository`
  so save/load/history uses the Phase 2 repository.
- Compile the full project and run the new module tests.
- Run `actionlint` after CI/Nightly edits.

## Risks

- Spring/Kafka/NATS adapters add optional compile-time surfaces. Keep them
  `compileOnly` and document that consumers must add the matching runtime
  dependency when using those adapters.
- Keep `javers-exposed` as a test/integration dependency for this module rather
  than a transitive API dependency; consumers can combine both artifacts through
  the BOM when they need Exposed persistence.
- JaVers shadow reconstruction requires aggregate classes to be JaVers-managed
  entities with stable ids. README examples must make that clear.
- The first version does not replace an outbox for exactly-once external
  delivery. Document it as immediate publisher adapters, not a durable outbox.

## Review Notes

- Historical external CLI review attempt was recorded under `.omx/artifacts`.
- Result: blocked by `API Error: 400 This organization has been disabled.`
- Local decision: proceed with implementation using current issue requirements,
  source inspection, compile/test validation, and local/native 7-tier review.
  This historical tool outage is not an active process gate.
