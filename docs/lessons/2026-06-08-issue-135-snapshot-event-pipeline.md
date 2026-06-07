# Issue #135 Snapshot Event Pipeline Lessons

## Context

Issue #135 added a transport-neutral JaVers snapshot event contract and adapted
the existing Kafka repositories to publish through event publishers.

## Decision

Keep `javers-core` transport-neutral and dependency-free. Put Kafka-specific
publishers and the governed `bluetape4k-kafka` producer helper usage in
`javers-persistence-kafka`.

For `data class` values that need constructor validation, use a private primary
constructor plus companion `operator fun invoke(...)`. Add
`@ConsistentCopyVisibility` so generated `copy()` follows constructor
visibility.

## Outcome

Spring Kafka and vanilla Kafka repositories now share `CdoSnapshotEvent<String>`
metadata and payload construction while preserving write-only behavior and
publish failure propagation.

## Verification

- `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`
- `git diff --check`

## Future Guard

When adding NATS, SQS, or another transport, implement only a transport adapter
around `CdoSnapshotEventPublisher<T>`. Do not put transport dependencies in
`javers-core`.
