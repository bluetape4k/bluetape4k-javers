# Issue #135 - Snapshot Event Pipeline 설계

## 맥락

Issue #135는 repository composition을 Kafka에 hard-code하지 않고 JaVers snapshot event가 Kafka, NATS JetStream, Amazon SQS, future transport를 통해 흐를 수 있도록 pluggable snapshot event pipeline을 요구한다.

현재 `develop` 근거:

- Base branch: `origin/develop@9c63a0d`.
- #136 is merged. `javers-persistence-kafka` now provides:
  - `KafkaCdoSnapshotRepository` for Spring Kafka `KafkaTemplate`.
  - `VanillaKafkaCdoSnapshotRepository` for Apache Kafka `Producer<String, String>`.
- 두 Kafka repository는 의도적으로 write-only다.
- `AbstractCdoSnapshotRepository.persist()`는 `saveSnapshot(snapshot)`을 호출하고 모든 publish가 성공한 뒤에만 repository head를 update한다.
- `saveSnapshot()`은 original `Commit`이 아니라 `CdoSnapshot`을 받으므로 transport metadata는 `CdoSnapshot` 및 `snapshot.commitMetadata`에서 파생해야 한다.
- Version catalog는 `bluetape4k-nats`를 포함하지만 이 repository에는 SQS / AWS SDK alias가 없다.
- Version catalog는 이미 `bluetape4k-kafka`를 포함한다. 이 PR은 raw Kafka client construction을 local로 wiring하지 않고 vanilla Kafka producer creation helper용 governed dependency를 사용한다.

## 범위

Transport-neutral snapshot event contract를 구현하고 기존 Kafka repository path를 이에 맞게 adapt한다.

Scope 포함:

- `javers-core`에 작은 public event model을 추가한다.
  - `CdoSnapshotEventMetadata`
  - `CdoSnapshotEvent<T>`
  - `CdoSnapshotEventPublisher<T>`
  - codec id constants for built-in snapshot-event payloads
- Metadata field:
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
- `javers-persistence-kafka`에 Kafka publisher adapter를 추가한다.
  - Spring Kafka adapter backed by `KafkaTemplate<String, String>`.
  - Vanilla Kafka adapter backed by Apache Kafka `Producer<String, String>`.
  - Vanilla producer factory backed by `bluetape4k-kafka` `producerOf(...)`.
- 현재 write-only behavior와 failure propagation을 보존하면서 `KafkaCdoSnapshotRepository` 및 `VanillaKafkaCdoSnapshotRepository`가 `CdoSnapshotEvent<String>`을 만들고 adapter에 publishing을 delegate하도록 refactor한다.
- README English/Korean locale pair를 transport selection guidance 및 NATS/SQS adapter design note로 갱신한다.
- 이 issue에서 NATS JetStream과 SQS는 새 runtime dependency가 아니라 testable adapter design으로 기록한다.

Scope 제외:

- 새 publishable module을 추가하지 않는다.
- 이 PR에서 NATS JetStream implementation을 추가하지 않는다.
- 이 PR에서 SQS/AWS SDK implementation을 추가하지 않는다.
- Read-capable Kafka projection을 추가하지 않는다. #105가 소유한다.
- Durable-history plus event-stream composition을 추가하지 않는다. #131이 소유한다.
- Retry, background queue, batching, metrics API, circuit breaker를 추가하지 않는다.

## Public API 계약

### `CdoSnapshotEventMetadata`

`CdoSnapshotEventMetadata`는 transport-neutral routing, ordering, idempotency, observability metadata를 담는 serializable value object다.

Rule:

- `globalIdValue`, `commitId`, `snapshotType`, `codecId`, `idempotencyKey`는 non-blank다.
- `snapshotVersion`은 positive다.
- 현재 base repository가 `saveSnapshot()` 성공 후 sequence를 assign하므로 `repositorySequence`는 nullable이다.
- `idempotencyKey`는 opaque이며 transport가 parse하면 안 된다. Default key는 동일 snapshot에 대해 stable하다: global id + commit id + snapshot version.
- `commitTimestamp`는 `snapshot.commitMetadata.commitDateInstant`를 사용한다.

### `CdoSnapshotEvent<T>`

`CdoSnapshotEvent<T>`는 metadata와 encoded payload를 결합한다. 현재 Kafka path는 plain JaVers JSON string codec과 함께 `T = String`을 사용한다.

### `CdoSnapshotEventPublisher<T>`

`CdoSnapshotEventPublisher<T>`는 synchronous contract다.

```kotlin
fun interface CdoSnapshotEventPublisher<T: Any> {
    fun publish(event: CdoSnapshotEvent<T>)
}
```

동작:

- `publish()`는 transport가 해당 adapter contract에 따라 event를 accept 또는 acknowledge한 뒤에만 반환한다.
- Failure는 exception으로 propagate된다.
- Caller는 transport-specific checked, timeout, interrupted error를 `RuntimeException`으로 wrap할 수 있지만 `InterruptedException` interrupt status는 보존해야 한다.
- Interface는 resource lifecycle을 소유하지 않는다. Adapter class는 explicit close ownership option으로 caller-owned client를 wrap할 때 `AutoCloseable`을 구현할 수 있다.

## Kafka Adapter Contract

Spring Kafka adapter:

- 기본적으로 template default topic에 publish한다.
- Caller가 key mapper를 제공하지 않으면 `event.metadata.globalIdValue`를 Kafka key로 사용한다.
- `event.payload`를 value로 전송한다.
- `publishTimeout`까지 block한다.
- `InterruptedException`에서 interrupt status를 restore한다.
- Repository head가 advance되지 않도록 publish failure를 propagate한다.

Vanilla Kafka adapter:

- `VanillaKafkaCdoSnapshotRepositoryOptions.topic`에 publish한다.
- Caller가 key mapper를 제공하지 않으면 `event.metadata.globalIdValue`를 Kafka key로 사용한다.
- Governed `bluetape4k-kafka` `producerOf(...)` helper를 통해 caller-supplied config에서 producer를 만들 수 있다.
- `event.payload`를 value로 전송한다.
- `publishTimeout`까지 block한다.
- Successful acknowledgement 후 optional하게 flush한다.
- `closeProducerOnClose = true`일 때만 producer를 close한다.

## NATS JetStream Design Artifact

NATS JetStream adapter 형태:

- Client surface: governed `bluetape4k-nats` dependency line의 caller-provided JetStream publishing client.
- Subject mapper: configuration의 default subject plus optional mapper.
- Payload: `event.payload`.
- Headers / metadata: global id, commit id, snapshot version, snapshot type,
  codec id, and idempotency key.
- Acknowledgement: `publish()` 반환 전에 publish acknowledgement를 받아야 한다.
- Failure behavior: publish timeout 또는 negative acknowledgement를 propagate한다.
- Ordering: Kafka partition ordering과 equivalent하지 않고 subject/stream-dependent로 문서화한다.
- Lifecycle: explicit ownership option이 나중에 추가되지 않는 한 caller가 NATS connection을 소유한다.

이 issue는 NATS implementation을 추가하지 않고 adapter contract를 기록한다.

## SQS Design Artifact

SQS adapter 형태:

- Client surface: 별도 dependency governance decision이 SDK alias를 추가한 뒤의 caller-provided AWS SDK SQS client.
- Queue URL: required, non-blank configuration.
- Payload: `event.payload`.
- Message attributes: global id, commit id, snapshot version, snapshot type,
  codec id, idempotency key.
- FIFO support: message group id와 deduplication id는 FIFO queue에서만 required이며 standard queue용 mandatory field로 노출하면 안 된다.
- Acknowledgement: `sendMessage` / `sendMessageBatch` success가 publish acknowledgement다.
- Failure behavior: client error, timeout, partial batch failure를 propagate한다.
- Ordering 및 retry semantics는 Kafka equivalent가 아니라 queue-type-specific으로 문서화한다.

이 issue는 SQS implementation을 추가하지 않고 adapter contract를 기록한다.

## 문서 요구사항

- `javers-persistence-kafka/README.md`를 갱신한다.
- `javers-persistence-kafka/README.ko.md`를 갱신한다.
- 다음을 문서화한다.
  - event pipeline contract
  - Kafka Spring vs vanilla adapter selection
  - NATS JetStream planned adapter semantics
  - SQS planned adapter semantics
  - why #105 and #131 remain separate

## Validation 요구사항

- Metadata extraction 및 default idempotency key용 unit test.
- 두 Kafka repository path의 publisher contract delegation용 unit test.
- Spring Kafka 및 vanilla Kafka adapter용 unit test:
  - key mapper
  - payload preservation
  - timeout/failure propagation
  - interrupt preservation
  - vanilla flush and close ownership
- 기존 Kafka repository behavior는 계속 통과해야 한다.
- Testcontainers-backed Kafka module test를 serial로 실행한다.
  - `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Kafka module의 production dependency check를 실행해 `bluetape4k-kafka`가 존재하고 새 NATS 또는 SQS/AWS SDK runtime dependency가 도입되지 않았음을 확인한다.
- `git diff --check`를 실행한다.

## Risk

- Public API breadth: core contract를 작고 transport-neutral하게 유지한다.
- Metadata availability: repository sequence는 publish 전 `saveSnapshot()` 안에서 알 수 없으므로 nullable이다.
- Semantics mismatch: README는 NATS 또는 SQS가 Kafka처럼 동작한다고 암시하면 안 된다.
- Dependency creep: Kafka helper에는 governed `bluetape4k-kafka` dependency를 사용하지만, 이 PR에서 AWS SDK/SQS 또는 NATS implementation dependency를 추가하지 않는다.
