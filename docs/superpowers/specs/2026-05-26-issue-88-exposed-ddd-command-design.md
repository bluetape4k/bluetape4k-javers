# Issue #88 — javers-exposed-ddd Command-Side Example Design

Date: 2026-05-26
Issue: https://github.com/bluetape4k/bluetape4k-javers/issues/88
Parent: https://github.com/bluetape4k/bluetape4k-javers/issues/5

## Context

Issue #5 asks for a CQRS / Event Sourcing example that combines JaVers,
Exposed, Kotlin, Kafka, and Redis. That scope is too large for one reviewable
PR, so it is split into command-side, projection-side, and benchmark/document
work. Issue #88 is the first slice.

The newly merged `javers-ddd` module provides:

- `AggregateRoot<ID>`
- `DomainEvent`
- `AggregateRepository<T, ID>`
- `DomainEventPublisher`

The newly merged `javers-exposed` module provides:

- `ExposedCdoSnapshotRepository`
- JaVers snapshot and commit tables for Exposed JDBC persistence

## Goal

Add an example module that demonstrates command-side aggregate persistence:

```text
OrderCommandHandler
  -> Exposed order table
  -> AggregateRepository.save(...)
  -> JaVers commit through ExposedCdoSnapshotRepository
  -> DomainEventPublisher
```

The example must be small enough to review independently and must not include
Kafka consumers, Redis projections, or benchmarks.

## Scope

- Add `javers-exposed-ddd` as a Gradle example module.
- Model an order aggregate with ID value type, command types, line items,
  status transitions, and domain events.
- Persist order state with Exposed JDBC tables.
- Integrate the order repository with `AggregateRepository`.
- Add an `OrderCommandHandler` that handles place/mark-paid commands.
- Add H2-backed tests proving:
  - command handler persists aggregate state;
  - JaVers stores snapshots and commit properties;
  - domain events are published after save;
  - repository load can reconstruct state from the source table.
- Update top-level docs, WIP, CI, and Nightly so the example stays exercised.

## Non-Goals

- Kafka event consumption and Redis read model projection. Covered by #89.
- Envers comparison benchmark. Covered by #90.
- Production outbox semantics. The example uses immediate publisher helpers
  from `javers-ddd`.
- Spring Boot auto-configuration. The first slice keeps framework wiring small
  and focuses on command-side domain flow.

## API Shape

The example module uses public classes under:

```text
io.bluetape4k.javers.examples.exposedddd
```

Planned types:

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

`Order` implements `AggregateRoot<OrderId>` and marks the JaVers ID property
with `@Id`.

## Persistence Shape

The command-side source table is intentionally simple:

- `example_order`
  - `id`
  - `customer_id`
  - `status`
  - `items_json`
  - `created_at`
  - `updated_at`

Line items are serialized as JSON for this slice. A normalized order item table
would distract from the JaVers/DDD integration that #88 is meant to prove.

## Validation

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## Risks

- JaVers treats Kotlin inline/value IDs and collections strictly. Tests must
  verify the mapped aggregate is an entity, not a value object.
- Example dependencies should not expand published library API. The example
  module can use implementation dependencies on `javers-ddd` and
  `javers-exposed`.
- CI path filters must include the example path or the module can silently skip
  tests.

## Review Notes

Claude advisor is expected to be unavailable in this environment because the
previous #4 run returned `API Error: 400 This organization has been disabled.`
If it still fails, record that as a validation gap and proceed with local review
and CI evidence.
