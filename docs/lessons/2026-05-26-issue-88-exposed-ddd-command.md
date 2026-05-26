# 2026-05-26 Issue #88 — javers-exposed-ddd command-side example

## Context

Parent issue #5 was split into #88, #89, and #90 to keep the CQRS example
reviewable. #88 owns only the command-side Exposed + JaVers + DDD helper flow.

## Decision

Use `examples/javers-exposed-ddd` as the file location while mapping the Gradle
project path to `:javers-exposed-ddd`. Keep Kafka, Redis, and benchmarks out of
the first slice.

## Outcome

The first example slice now persists an `Order` aggregate through Exposed,
commits snapshots through `ExposedCdoSnapshotRepository`, and publishes
`DomainEvent` instances through `AggregateRepository`.

## Verification

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`

## Future Guidance

#89 should add Kafka-to-Redis projection tests serially because those tests will
use Testcontainers. #90 should record raw benchmark artifacts before updating
README claims.
