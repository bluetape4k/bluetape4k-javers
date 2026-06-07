# Issue #135 - Snapshot Event Pipeline Design

## Context

Issue #135 asks for a pluggable snapshot event pipeline so JaVers snapshot
events can flow through Kafka, NATS JetStream, Amazon SQS, and future
transports without hard-coding repository composition to Kafka.

Current `develop` evidence:

- Base branch: `origin/develop@9c63a0d`.
- #136 is merged. `javers-persistence-kafka` now provides:
  - `KafkaCdoSnapshotRepository` for Spring Kafka `KafkaTemplate`.
  - `VanillaKafkaCdoSnapshotRepository` for Apache Kafka `Producer<String, String>`.
- Both Kafka repositories are intentionally write-only.
- `AbstractCdoSnapshotRepository.persist()` calls `saveSnapshot(snapshot)` and
  updates the repository head only after every publish succeeds.
- `saveSnapshot()` receives a `CdoSnapshot`, not the original `Commit`, so
  transport metadata must be derived from `CdoSnapshot` and
  `snapshot.commitMetadata`.
- The version catalog contains `bluetape4k-nats`; it does not contain an SQS /
  AWS SDK alias in this repository.
- The version catalog already contains `bluetape4k-kafka`; this PR uses that
  governed dependency for vanilla Kafka producer creation helpers instead of
  wiring raw Kafka client construction locally.

## Scope

Implement a transport-neutral snapshot event contract and adapt the existing
Kafka repository paths to it.

In scope:

- Add a small public event model in `javers-core`:
  - `CdoSnapshotEventMetadata`
  - `CdoSnapshotEvent<T>`
  - `CdoSnapshotEventPublisher<T>`
  - codec id constants for built-in snapshot-event payloads
- Metadata fields:
  - global id value
  - commit id string
  - commit major id
  - commit minor id
  - repository sequence when available
  - snapshot version
  - snapshot type
  - author
  - commit timestamp
  - codec id
  - idempotency key
- Add Kafka publisher adapters in `javers-persistence-kafka`:
  - Spring Kafka adapter backed by `KafkaTemplate<String, String>`.
  - Vanilla Kafka adapter backed by Apache Kafka `Producer<String, String>`.
  - Vanilla producer factory backed by `bluetape4k-kafka` `producerOf(...)`.
- Refactor `KafkaCdoSnapshotRepository` and
  `VanillaKafkaCdoSnapshotRepository` to build `CdoSnapshotEvent<String>` and
  delegate publishing to the adapters while preserving current write-only
  behavior and failure propagation.
- Update README English/Korean locale pair with transport selection guidance and
  NATS/SQS adapter design notes.
- Record NATS JetStream and SQS as testable adapter designs, not new runtime
  dependencies, in this issue.

Out of scope:

- No new publishable module.
- No NATS JetStream implementation in this PR.
- No SQS/AWS SDK implementation in this PR.
- No read-capable Kafka projection; #105 owns that.
- No durable-history plus event-stream composition; #131 owns that.
- No retries, background queues, batching, metrics API, or circuit breaker.

## Public API Contract

### `CdoSnapshotEventMetadata`

`CdoSnapshotEventMetadata` is a serializable value object that carries
transport-neutral routing, ordering, idempotency, and observability metadata.

Rules:

- `globalIdValue`, `commitId`, `snapshotType`, `codecId`, and
  `idempotencyKey` are non-blank.
- `snapshotVersion` is positive.
- `repositorySequence` is nullable because the current base repository assigns
  its sequence after `saveSnapshot()` succeeds.
- `idempotencyKey` is opaque and must not be parsed by transports. The default
  key is stable for the same snapshot: global id + commit id + snapshot version.
- `commitTimestamp` uses `snapshot.commitMetadata.commitDateInstant`.

### `CdoSnapshotEvent<T>`

`CdoSnapshotEvent<T>` combines metadata and an encoded payload. Current Kafka
paths use `T = String` with the plain JaVers JSON string codec.

### `CdoSnapshotEventPublisher<T>`

`CdoSnapshotEventPublisher<T>` is a synchronous contract:

```kotlin
fun interface CdoSnapshotEventPublisher<T: Any> {
    fun publish(event: CdoSnapshotEvent<T>)
}
```

Behavior:

- `publish()` returns only after the transport accepts or acknowledges the
  event according to that adapter's contract.
- Failures are propagated as exceptions.
- Callers may wrap transport-specific checked, timeout, or interrupted errors in
  `RuntimeException`, but must preserve `InterruptedException` interrupt status.
- The interface does not own resource lifecycle. Adapter classes may implement
  `AutoCloseable` when they wrap a caller-owned client with explicit close
  ownership options.

## Kafka Adapter Contract

Spring Kafka adapter:

- Publishes to the template default topic by default.
- Uses `event.metadata.globalIdValue` as the Kafka key unless a caller supplies
  a key mapper.
- Sends `event.payload` as the value.
- Blocks up to `publishTimeout`.
- Restores interrupt status on `InterruptedException`.
- Propagates publish failures so repository head is not advanced.

Vanilla Kafka adapter:

- Publishes to `VanillaKafkaCdoSnapshotRepositoryOptions.topic`.
- Uses `event.metadata.globalIdValue` as the Kafka key unless a caller supplies
  a key mapper.
- Can create a producer from caller-supplied config through the governed
  `bluetape4k-kafka` `producerOf(...)` helper.
- Sends `event.payload` as the value.
- Blocks up to `publishTimeout`.
- Optionally flushes after successful acknowledgement.
- Closes the producer only when `closeProducerOnClose = true`.

## NATS JetStream Design Artifact

NATS JetStream adapter shape:

- Client surface: a caller-provided JetStream publishing client from the
  governed `bluetape4k-nats` dependency line.
- Subject mapper: default subject from configuration plus optional mapper.
- Payload: `event.payload`.
- Headers / metadata: global id, commit id, snapshot version, snapshot type,
  codec id, and idempotency key.
- Acknowledgement: publish acknowledgement must be received before `publish()`
  returns.
- Failure behavior: publish timeout or negative acknowledgement is propagated.
- Ordering: documented as subject/stream-dependent, not equivalent to Kafka
  partition ordering.
- Lifecycle: caller owns the NATS connection unless an explicit ownership option
  is added later.

This issue records the adapter contract without adding a NATS implementation.

## SQS Design Artifact

SQS adapter shape:

- Client surface: caller-provided AWS SDK SQS client after a separate dependency
  governance decision adds the SDK alias.
- Queue URL: required, non-blank configuration.
- Payload: `event.payload`.
- Message attributes: global id, commit id, snapshot version, snapshot type,
  codec id, idempotency key.
- FIFO support: message group id and deduplication id are required only for FIFO
  queues and must not be exposed as mandatory fields for standard queues.
- Acknowledgement: `sendMessage` / `sendMessageBatch` success is the publish
  acknowledgement.
- Failure behavior: client error, timeout, or partial batch failure is
  propagated.
- Ordering and retry semantics are documented as queue-type-specific, not Kafka
  equivalent.

This issue records the adapter contract without adding an SQS implementation.

## Documentation Requirements

- Update `javers-persistence-kafka/README.md`.
- Update `javers-persistence-kafka/README.ko.md`.
- Document:
  - event pipeline contract
  - Kafka Spring vs vanilla adapter selection
  - NATS JetStream planned adapter semantics
  - SQS planned adapter semantics
  - why #105 and #131 remain separate

## Validation Requirements

- Unit tests for metadata extraction and default idempotency key.
- Unit tests for publisher contract delegation in both Kafka repository paths.
- Unit tests for Spring Kafka and vanilla Kafka adapters:
  - key mapper
  - payload preservation
  - timeout/failure propagation
  - interrupt preservation
  - vanilla flush and close ownership
- Existing Kafka repository behavior must remain passing.
- Run Testcontainers-backed Kafka module tests serially:
  - `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Run production dependency check for the Kafka module to confirm
  `bluetape4k-kafka` is present and no new NATS or SQS/AWS SDK runtime
  dependency is introduced.
- Run `git diff --check`.

## Risks

- Public API breadth: keep the core contract small and transport-neutral.
- Metadata availability: repository sequence is nullable because it is not known
  inside `saveSnapshot()` before publish.
- Semantics mismatch: README must not imply NATS or SQS behaves like Kafka.
- Dependency creep: use the governed `bluetape4k-kafka` dependency for Kafka
  helpers, but do not add AWS SDK/SQS or NATS implementation dependencies in
  this PR.
