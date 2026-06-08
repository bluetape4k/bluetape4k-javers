# Issue #105 - Kafka Audit Projection Lesson

## Context

Kafka snapshot repositories are intentionally write-only, but #105 needed a
read-capable audit path.

## Decision

Keep Kafka repositories as publishers and introduce a separate projector that
replays Kafka records into an existing `CdoSnapshotRepository`.

## Outcome

The projection path reuses bluetape4k-kafka consumers, existing JaVers codecs,
Redis/Caffeine repository targets, and Testcontainers launchers without adding a
new storage abstraction.

## Verification

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Result: `SUCCESS: Executed 39 tests in 10.9s`

## Future Guidance

For #131, compose durable read storage plus Kafka publishing explicitly. Do not
make the Kafka repositories silently read-capable.
