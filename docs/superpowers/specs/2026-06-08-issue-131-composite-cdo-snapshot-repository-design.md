# Issue #131 - Composite CDO Snapshot Repository Design

## Goal

Add a first-class composite `CdoSnapshotRepository` so applications can keep a
durable read/query store and fan out the same JaVers snapshots to optional
secondary repositories such as Kafka or Redis.

The first implementation must stay provider-neutral and live in `javers-core`.
It should compose existing `CdoSnapshotRepository` implementations instead of
adding a JaVers-only cache abstraction or introducing dependencies on
`javers-exposed`, `javers-persistence-redis`, or `javers-persistence-kafka`.

## Evidence

- GitHub issue #131 requires a durable history plus event stream composition.
- `develop` is at `e54336d docs: add Kafka projection diagram`.
- `CdoSnapshotRepository` is already the common storage contract for core,
  Exposed, Redis, and Kafka modules.
- `AbstractCdoSnapshotRepository.persist()` calls `saveSnapshot(snapshot)` for
  every snapshot and advances the repository head only after every snapshot save
  succeeds.
- Kafka repositories remain intentionally write-only. #105 / PR #185 added
  `KafkaCdoSnapshotProjector` for explicit read-side replay into a supplied
  read-capable `CdoSnapshotRepository`.
- #133 documents that canonical JaVers audit snapshot/head state is unsafe for
  generic cache write-behind. Cache/near-cache behavior should use existing
  `bluetape4k-exposed` cache contracts for application read models or
  projections, not a new JaVers cache layer.
- `javers-core` already has `CompositeDispatcher`, but it is best-effort and
  domain-event oriented. Snapshot persistence needs an explicit failure policy.

## Public API

Add these public types under `io.bluetape4k.javers.repository.composite`:

- `CompositeCdoSnapshotRepository`
- `CompositeCdoSnapshotRepositoryOptions`
- `CompositeCdoSnapshotFailurePolicy`
- `CompositeCdoSnapshotDelegateKind`
- `CompositeCdoSnapshotWriteFailure`
- `CompositeCdoSnapshotException`

### `CompositeCdoSnapshotRepository`

Constructor contract:

- Requires one primary `CdoSnapshotRepository`.
- Accepts zero or more secondary `CdoSnapshotRepository` instances.
- Reads delegate only to the primary repository.
- Writes save to the primary first, then fan out to secondaries in the supplied
  order.
- `persist(commit)` calls `primary.persist(commit)` first so the durable
  repository keeps its native head/sequence behavior, then calls
  `persist(commit)` on each secondary according to the failure policy.
- `setJsonConverter()` is propagated to the primary and all secondaries.
- `ensureSchema()` is propagated to the primary and all secondaries using the
  same failure policy as writes.
- `getHeadId()` delegates to the primary.
- `AutoCloseable.close()` closes delegates that implement `AutoCloseable`.
  Close is best-effort and must try every closeable delegate, then throw a
  combined exception when any close fails.

The repository must not extend `AbstractCdoSnapshotRepository` because it does
not own a codec, head sequence, or snapshot serialization. It should implement
`CdoSnapshotRepository` directly and delegate query behavior to the primary.

### `CompositeCdoSnapshotRepositoryOptions`

Options:

- `writeFailurePolicy`: default `FAIL_FAST`.
- `ensureSchemaFailurePolicy`: default `FAIL_FAST`.
- `closeFailurePolicy`: default `BEST_EFFORT`.

Use a private constructor plus companion `operator fun invoke(...)` when
validation is required. Keep options serializable.

### Failure policy

`CompositeCdoSnapshotFailurePolicy`:

- `FAIL_FAST`: propagate the first delegate failure and stop remaining
  secondary writes. This is the default because it preserves the JaVers
  native `persist()` contract for the primary repository.
- `BEST_EFFORT`: try all secondary writes and throw an aggregate exception only
  after all delegates have been attempted. This policy is allowed for event
  streams or rebuildable projections when callers accept partial secondary
  failure. The primary write still happens first and still fails fast.

The primary write always fails fast regardless of policy. If primary storage is
unavailable, no secondary repository should receive the snapshot.

### Failure model

`CompositeCdoSnapshotWriteFailure` records:

- delegate kind: `PRIMARY` or `SECONDARY`
- delegate index
- delegate class name
- operation name
- cause

`CompositeCdoSnapshotException` records all failures and exposes the first
failure as its cause. Messages must include delegate kind/index/class name and
operation, but must not include raw snapshot global-id values.

## Behavior Contract

- Public read methods (`getLatest`, `getStateHistory`, `getValueObjectStateHistory`,
  `getSnapshots`, `loadSnapshots`, and `getHeadId`) delegate to primary storage.
- `saveSnapshot(snapshot)` writes primary first and then secondaries in order.
- `persist(commit)` writes primary first using the primary repository's native
  `persist()` implementation, then secondaries in order.
- The composite must not claim atomicity across primary and secondaries.
  Distributed transaction, outbox, retry, and compensation are non-goals for
  this issue.
- If a secondary fails after the primary succeeds, the primary may already
  contain the commit and expose its head. The composite surfaces the secondary
  failure but does not roll back primary storage.
- Duplicate secondary entries are caller responsibility unless the secondary
  repository itself is idempotent.
- Redis/cache integration is represented by passing an existing Redis-backed or
  projection-backed `CdoSnapshotRepository` as primary or secondary. New generic
  cache modes are not added in this issue.
- Kafka integration is represented by passing
  `KafkaCdoSnapshotRepository` or `VanillaKafkaCdoSnapshotRepository` as a
  secondary write repository. Kafka read methods remain write-only.

## Recommended Compositions

| Shape | Primary | Secondary repositories | Notes |
|---|---|---|---|
| Durable + events | Exposed | Kafka | SQL remains query source; Kafka gets audit events. |
| Durable + Redis projection + events | Exposed | Redis, Kafka | Redis is explicit projection/cache store, not hidden write-behind. |
| Redis-first cache store + events | Redis | Kafka | Useful when Redis is accepted as the direct JaVers snapshot repository. |
| In-memory tests + events | Caffeine | Mock/Kafka test repository | Fast unit-test shape. |

## Documentation Requirements

- Update `javers-core/README.md` and `javers-core/README.ko.md` with:
  - composite repository overview
  - Exposed + Kafka example
  - Exposed + Redis + Kafka guidance
  - failure policy explanation
  - explicit non-atomicity and Kafka write-only notes
- Update root `README.md` / `README.ko.md` only if the module capability list
  needs a short cross-reference.
- Public KDoc must be English and include a Kotlin usage example.

## Test Requirements

Unit tests in `javers-core`:

- Options validation and defaults.
- Primary read delegation for representative read methods.
- `setJsonConverter()` and `ensureSchema()` propagation.
- Primary save happens before secondary saves.
- Primary failure prevents secondary writes.
- `FAIL_FAST` secondary failure stops later secondaries and throws.
- `BEST_EFFORT` secondary failure attempts all secondaries and throws aggregate
  failures afterward.
- `close()` attempts all closeable delegates and reports aggregate failures.
- JaVers commit path with Caffeine primary plus recording secondaries verifies
  snapshots are saved and latest reads come from primary.

Cross-module preservation tests:

- Keep existing Kafka write-only tests unchanged.
- Run `:javers-persistence-kafka:test` if the docs or tests instantiate Kafka
  repositories with the composite.
- Run Redis/Exposed tests only when their source or test fixtures are touched.

## Validation Commands

Run serially:

```bash
./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain
./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain
git diff --check
```

If only `javers-core` source and docs change and Kafka module source is
untouched, `:javers-persistence-kafka:test` is still useful as a contract guard
because Kafka write-only repositories are the main secondary use case.

## Non-goals

- New module.
- New dependency.
- New cache abstraction in `bluetape4k-javers`.
- Automatic outbox, retry queue, compensation, transaction manager, or exactly
  once semantics.
- Making Kafka repositories read-capable.
- Spring Boot auto-configuration; #104 owns that.
- Ktor example changes. Future Ktor work should reuse `bluetape4k-projects`
  Ktor modules when needed.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Users assume atomic multi-store writes | KDoc/README state non-atomic fanout and failure policies. |
| Secondary event stream receives writes when durable store fails | Primary is always written first and always fails fast. |
| Hidden cache semantics duplicate `bluetape4k-exposed` | Composite only accepts existing repositories; no new cache API. |
| Best-effort hides failures | Best-effort throws aggregate failures after attempts; it never silently swallows failures. |
| Sensitive snapshot identifiers leak in errors | Failure messages use delegate kind/index/class name, not raw snapshot keys. |
| Close failure prevents later delegates from closing | Close uses best-effort attempt-all behavior. |
