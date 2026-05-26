# Issue 4 javers-ddd Helper Module

## Context

Issue #4 follows `javers-exposed` and adds a DDD helper layer for aggregate roots,
domain events, JaVers commit/history access, and event publisher adapters.

## Decision

Use an extensible `DomainEvent` interface instead of the issue's sealed-class
sketch because library consumers must define event types outside this module.
Keep aggregate persistence abstract and let consumers provide Exposed/Spring
Data/hand-written persistence hooks. Use JaVers for commit/history/shadow
loading, not as the primary aggregate store.

## Outcome

Added `javers-ddd` with aggregate contracts, event property mapping,
`AggregateRepository`, no-op/function/composite publishers, and optional
Spring/Kafka/NATS adapters. Tests cover event mapping, publisher dispatch, and
H2 + `ExposedCdoSnapshotRepository` aggregate save/load/history.

## Verification

- `./gradlew :javers-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-ddd:cleanTest :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## Follow-Up

JaVers aggregate types need explicit entity id mapping such as `@Id` on the
aggregate id property plus `registerEntity(...)`. Keep this visible in examples
and in the Phase 4 CQRS/Event Sourcing demo.
