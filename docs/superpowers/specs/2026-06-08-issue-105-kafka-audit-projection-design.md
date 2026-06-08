# Issue #105 - Kafka Audit Projection Design

## Goal

Add an explicit read-side Kafka projection path for JaVers CDO snapshots while
preserving the write-only contract of the Kafka repositories.

## Evidence

- GitHub issue #105 requires a read-capable Kafka audit projection path.
- #135 and #136 have landed. Kafka repositories now publish encoded snapshot
  payloads through a shared event pipeline and vanilla Kafka publisher.
- `KafkaCdoSnapshotRepository` and `VanillaKafkaCdoSnapshotRepository` remain
  intentionally write-only.
- #89 already demonstrates Kafka events plus Redis projection at example level,
  but that code is order-domain-specific and should not become the reusable
  audit projection API.
- `javers-persistence-redis`, `javers-exposed`, and `javers-core` already expose
  read-capable `CdoSnapshotRepository` implementations. The projection should
  reuse that contract instead of adding another storage abstraction.
- `bluetape4k-kafka` already provides `consumerOf(...)` and `producerOf(...)`
  helpers. `javers-persistence-kafka` already depends on it.

## Public API

- Add `KafkaCdoSnapshotProjectionOptions`.
  - `topic` is the source Kafka topic and must be non-blank.
  - `pollTimeout` must be positive.
  - `subscribeOnStart` defaults to `true`.
  - `commitOffsetsAfterProjection` defaults to `true`.
  - `skipExistingSnapshots` defaults to `true` for idempotent replay.
  - `closeConsumerOnClose` defaults to `false` for caller-owned consumers.
- Add `KafkaCdoSnapshotProjectionResult`.
  - Counts polled records, projected snapshots, and skipped snapshots.
- Add `KafkaCdoSnapshotProjector`.
  - Accepts a Kafka `Consumer<String, String>`, a JaVers `JsonConverter`, a
    target `CdoSnapshotRepository`, and options.
  - Offers convenience constructors that create a repository-owned consumer with
    bluetape4k-kafka `consumerOf(...)`.
  - Implements `AutoCloseable` only for optional consumer ownership.

## Behavior Contract

- Kafka repositories stay write-only. No existing repository read method changes.
- Projection decodes the current Kafka wire value: encoded JaVers snapshot JSON
  payload, not a metadata envelope.
- The target repository is a supplied `CdoSnapshotRepository`. Redis, Exposed,
  Caffeine, or another implementation can be used without Kafka module changes.
- Projection applies records in deterministic `partition, offset` order within
  each polled batch.
- Total audit ordering is deterministic only when the source topic is configured
  so Kafka itself provides a total order, for example a single-partition topic or
  a partitioning strategy that keeps each aggregate on one partition.
- With `skipExistingSnapshots=true`, replay checks the target repository for the
  same `globalId`, `commitId`, and snapshot version before saving. This makes
  rebuild/retry idempotent for already projected snapshot payloads.
- If decode or target save fails, the exception is propagated. Offsets are
  committed only after the whole batch is projected successfully.

## Documentation

- Update `javers-persistence-kafka/README.md` and `README.ko.md`.
- Show the CQRS shape: Kafka repository for write stream, read-capable
  `CdoSnapshotRepository` for materialized reads, and projector for replay.
- Document duplicate handling, offset commit behavior, and ordering limits.

## Test Requirements

- Unit tests:
  - Options validation.
  - Convenience constructor uses bluetape4k-kafka consumer helper and closes a
    repository-owned consumer.
  - Idempotent replay skips duplicate snapshots.
  - Decode or save failures prevent offset commit.
- Integration tests:
  - Publish snapshots with `VanillaKafkaCdoSnapshotRepository`.
  - Rebuild a Redis `LettuceCdoSnapshotRepository` projection from Kafka records
    using `KafkaServer.Launcher` and `RedisServer.Launcher`.
- Targeted Gradle verification:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`.

## Non-goals

- Making Kafka repositories read-capable.
- Composite durable history plus event stream repository (#131).
- Spring Boot auto-configuration (#104).
- New Kafka metadata envelope or header contract.
- Kafka transactions, outbox, or exactly-once processing.
- Ktor example changes. If a future Ktor example consumes this projection, it
  should reuse `bluetape4k-projects` Ktor modules.

## Risks and Mitigations

- Risk: Replaying to a read store duplicates snapshots. Mitigation:
  `skipExistingSnapshots=true` checks target state before saving.
- Risk: Cross-partition ordering is misunderstood as a total order. Mitigation:
  document Kafka ordering limits and apply a deterministic batch order.
- Risk: Offsets are committed after partial projection. Mitigation: commit only
  after the full batch succeeds.
- Risk: Projection couples Kafka to Redis. Mitigation: target the existing
  `CdoSnapshotRepository` interface and use Redis only as integration evidence.
