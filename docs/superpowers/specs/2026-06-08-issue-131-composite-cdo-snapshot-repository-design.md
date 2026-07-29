# Issue #131 - Composite CDO Snapshot Repository 설계

## 목표

Application이 durable read/query store를 유지하면서 같은 JaVers snapshot을 Kafka 또는 Redis 같은 optional secondary repository로 fan out할 수 있도록 first-class composite `CdoSnapshotRepository`를 추가한다.

첫 구현은 provider-neutral이어야 하며 `javers-core`에 위치해야 한다. JaVers-only cache abstraction을 추가하거나 `javers-exposed`, `javers-persistence-redis`, `javers-persistence-kafka` dependency를 도입하지 않고 기존 `CdoSnapshotRepository` implementation을 compose해야 한다.

## 근거

- GitHub issue #131은 durable history plus event stream composition을 요구한다.
- `develop`은 `e54336d docs: add Kafka projection diagram`에 있다.
- `CdoSnapshotRepository`는 이미 core, Exposed, Redis, Kafka module의 common storage contract다.
- `AbstractCdoSnapshotRepository.persist()`는 모든 snapshot에 대해 `saveSnapshot(snapshot)`을 호출하고 모든 snapshot save가 성공한 뒤에만 repository head를 advance한다.
- Kafka repository는 의도적으로 write-only로 남는다. #105 / PR #185는 supplied read-capable `CdoSnapshotRepository`로 explicit read-side replay를 수행하는 `KafkaCdoSnapshotProjector`를 추가했다.
- #133은 canonical JaVers audit snapshot/head state가 generic cache write-behind에 unsafe하다고 문서화한다. Cache/near-cache behavior는 새 JaVers cache layer가 아니라 application read model 또는 projection용 기존 `bluetape4k-exposed` cache contract를 사용해야 한다.
- `javers-core`에는 이미 `CompositeDispatcher`가 있지만 best-effort이고 domain-event oriented다. Snapshot persistence에는 explicit failure policy가 필요하다.

## Public API

`io.bluetape4k.javers.repository.composite` 아래 다음 public type을 추가한다.

- `CompositeCdoSnapshotRepository`
- `CompositeCdoSnapshotRepositoryOptions`
- `CompositeCdoSnapshotFailurePolicy`
- `CompositeCdoSnapshotDelegateKind`
- `CompositeCdoSnapshotWriteFailure`
- `CompositeCdoSnapshotException`

### `CompositeCdoSnapshotRepository`

Constructor 계약:

- 하나의 primary `CdoSnapshotRepository`를 요구한다.
- Zero or more secondary `CdoSnapshotRepository` instance를 받는다.
- Read는 primary repository에만 delegate한다.
- Write는 primary에 먼저 save한 뒤 supplied order대로 secondary에 fan out한다.
- `persist(commit)`은 durable repository가 native head/sequence behavior를 유지하도록 먼저 `primary.persist(commit)`을 호출한 뒤, failure policy에 따라 각 secondary의 `persist(commit)`을 호출한다.
- `setJsonConverter()`는 primary와 모든 secondary에 propagate된다.
- `ensureSchema()`는 write와 동일한 failure policy를 사용해 primary와 모든 secondary에 propagate된다.
- `getHeadId()`는 primary에 delegate한다.
- `AutoCloseable.close()`는 `AutoCloseable`을 구현한 delegate를 close한다. Close는 best-effort이며 모든 closeable delegate를 시도한 뒤 close failure가 있으면 combined exception을 throw해야 한다.

이 repository는 codec, head sequence, snapshot serialization을 소유하지 않으므로 `AbstractCdoSnapshotRepository`를 확장하면 안 된다. `CdoSnapshotRepository`를 직접 구현하고 query behavior를 primary에 delegate해야 한다.

### `CompositeCdoSnapshotRepositoryOptions`

Option:

- `writeFailurePolicy`: default `FAIL_FAST`.
- `ensureSchemaFailurePolicy`: default `FAIL_FAST`.
- `closeFailurePolicy`: default `BEST_EFFORT`.

Validation이 필요하면 private constructor와 companion `operator fun invoke(...)`를 사용한다. Option은 serializable하게 유지한다.

### Failure policy

`CompositeCdoSnapshotFailurePolicy`:

- `FAIL_FAST`: 첫 delegate failure를 propagate하고 남은 secondary write를 중단한다. Primary repository에 대한 JaVers native `persist()` contract를 보존하므로 default다.
- `BEST_EFFORT`: 모든 secondary write를 시도하고 모든 delegate 시도 후에만 aggregate exception을 throw한다. Caller가 partial secondary failure를 수용하는 event stream 또는 rebuildable projection에 허용된다. Primary write는 여전히 먼저 발생하며 fail fast한다.

Primary write는 policy와 관계없이 항상 fail fast한다. Primary storage를 사용할 수 없으면 어떤 secondary repository도 snapshot을 받으면 안 된다.

### Failure model

`CompositeCdoSnapshotWriteFailure`는 다음을 기록한다.

- delegate kind: `PRIMARY` or `SECONDARY`
- delegate index
- delegate class name
- operation name
- cause

`CompositeCdoSnapshotException`은 모든 failure를 기록하고 첫 failure를 cause로 노출한다. Message는 delegate kind/index/class name 및 operation을 포함해야 하지만 raw snapshot global-id value는 포함하면 안 된다.

## Behavior 계약

- Public read method(`getLatest`, `getStateHistory`, `getValueObjectStateHistory`, `getSnapshots`, `loadSnapshots`, `getHeadId`)는 primary storage에 delegate한다.
- `saveSnapshot(snapshot)`은 primary를 먼저 write한 뒤 secondary를 순서대로 write한다.
- `persist(commit)`은 primary repository의 native `persist()` implementation을 사용해 primary를 먼저 write한 뒤 secondary를 순서대로 write한다.
- Composite는 primary와 secondary 사이의 atomicity를 주장하면 안 된다. Distributed transaction, outbox, retry, compensation은 이 issue의 non-goal이다.
- Primary가 성공한 뒤 secondary가 실패하면 primary는 이미 commit을 포함하고 head를 expose할 수 있다. Composite는 secondary failure를 surface하지만 primary storage를 roll back하지 않는다.
- Secondary repository 자체가 idempotent하지 않으면 duplicate secondary entry는 caller responsibility다.
- Redis/cache integration은 기존 Redis-backed 또는 projection-backed `CdoSnapshotRepository`를 primary 또는 secondary로 전달하는 방식으로 표현한다. 이 issue에서는 새 generic cache mode를 추가하지 않는다.
- Kafka integration은 `KafkaCdoSnapshotRepository` 또는 `VanillaKafkaCdoSnapshotRepository`를 secondary write repository로 전달하는 방식으로 표현한다. Kafka read method는 write-only로 남는다.

## 권장 composition

| 형태 | Primary | Secondary repository | 비고 |
|---|---|---|---|
| Durable + events | Exposed | Kafka | SQL이 query source로 남고 Kafka가 audit event를 받는다. |
| Durable + Redis projection + events | Exposed | Redis, Kafka | Redis는 hidden write-behind가 아니라 explicit projection/cache store다. |
| Redis-first cache store + events | Redis | Kafka | Redis가 direct JaVers snapshot repository로 허용될 때 유용하다. |
| In-memory tests + events | Caffeine | Mock/Kafka test repository | 빠른 unit-test 형태. |

## 문서 요구사항

- `javers-core/README.md`와 `javers-core/README.ko.md`를 다음 내용으로 갱신한다.
  - composite repository overview
  - Exposed + Kafka example
  - Exposed + Redis + Kafka guidance
  - failure policy explanation
  - explicit non-atomicity and Kafka write-only notes
- Module capability list에 짧은 cross-reference가 필요할 때만 root `README.md` / `README.ko.md`를 갱신한다.
- Public KDoc은 English여야 하며 Kotlin usage example을 포함해야 한다.

## 테스트 요구사항

`javers-core`의 unit test:

- Option validation 및 default.
- Representative read method에 대한 primary read delegation.
- `setJsonConverter()` 및 `ensureSchema()` propagation.
- Primary save가 secondary save보다 먼저 발생한다.
- Primary failure가 secondary write를 방지한다.
- `FAIL_FAST` secondary failure가 이후 secondary를 중단하고 throw한다.
- `BEST_EFFORT` secondary failure가 모든 secondary를 시도한 뒤 aggregate failure를 throw한다.
- `close()`가 모든 closeable delegate를 시도하고 aggregate failure를 보고한다.
- Caffeine primary와 recording secondary를 사용한 JaVers commit path가 snapshot이 저장되고 latest read가 primary에서 오는지 검증한다.

Cross-module preservation test:

- 기존 Kafka write-only test는 변경하지 않고 유지한다.
- Docs 또는 test가 composite와 함께 Kafka repository를 instantiate하면 `:javers-persistence-kafka:test`를 실행한다.
- Redis/Exposed test는 해당 source 또는 test fixture가 touched될 때만 실행한다.

## 검증 Command

Serial로 실행한다.

```bash
./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain
./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain
git diff --check
```

`javers-core` source와 docs만 변경되고 Kafka module source가 untouched라도 `:javers-persistence-kafka:test`는 contract guard로 여전히 유용하다. Kafka write-only repository가 주요 secondary use case이기 때문이다.

## Non-goal

- New module.
- New dependency.
- New cache abstraction in `bluetape4k-javers`.
- Automatic outbox, retry queue, compensation, transaction manager, exactly once semantics.
- Making Kafka repositories read-capable.
- Spring Boot auto-configuration. #104가 소유한다.
- Ktor example change. Future Ktor work는 필요할 때 `bluetape4k-projects` Ktor module을 재사용해야 한다.

## Risk 및 Mitigation

| Risk | Mitigation |
|---|---|
| User가 atomic multi-store write를 가정한다 | KDoc/README가 non-atomic fanout 및 failure policy를 명시한다. |
| Durable store가 실패했는데 secondary event stream이 write를 받는다 | Primary는 항상 먼저 write되고 항상 fail fast한다. |
| Hidden cache semantics가 `bluetape4k-exposed`를 중복한다 | Composite는 기존 repository만 받고 새 cache API를 추가하지 않는다. |
| Best-effort가 failure를 숨긴다 | Best-effort는 시도 후 aggregate failure를 throw하며 failure를 조용히 삼키지 않는다. |
| Sensitive snapshot identifier가 error에 leak된다 | Failure message는 raw snapshot key가 아니라 delegate kind/index/class name을 사용한다. |
| Close failure가 이후 delegate close를 막는다 | Close는 best-effort attempt-all behavior를 사용한다. |
