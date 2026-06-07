# Issue #135 - Snapshot Event Pipeline Plan

## Objective

Introduce a transport-neutral JaVers snapshot event publishing contract and
adapt the existing Kafka repository paths to it while using the governed
`bluetape4k-kafka` helper dependency for vanilla Kafka producer creation.

## Step 0/1 Evidence

- Worktree: `.worktrees/feat-issue-135-snapshot-event-pipeline`.
- Base: `origin/develop@9c63a0d`.
- Issue #135 body refreshed on 2026-06-08 KST.
- Current implementation: Spring Kafka and vanilla Kafka repositories are
  write-only and publish encoded string snapshots.
- Research: `docs/research/2026-06-04-javers-multilayer-cache-pipeline.md`.
- User scope update: use `bluetape4k-kafka` instead of keeping raw Kafka client
  helper construction outside the module dependency graph.

## Implementation Tasks

1. Add core event contract.
   - Create `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/event/`.
   - Add `CdoSnapshotEventMetadata`, `CdoSnapshotEvent<T>`,
     `CdoSnapshotEventPublisher<T>`, and codec id constants.
   - Include English KDoc and examples.
   - Use bluetape4k validation helpers for non-blank values.
   - Make serializable value objects define `serialVersionUID`.

2. Add core tests.
   - Test metadata extraction from committed `CdoSnapshot`.
   - Test idempotency key stability.
   - Test nullable repository sequence behavior.
   - Use bluetape4k assertions only.

3. Add Kafka publisher adapters.
   - Add `KafkaSnapshotEventPublisher` backed by `KafkaTemplate<String, String>`.
   - Add `VanillaKafkaSnapshotEventPublisher` backed by
     `Producer<String, String>`.
   - Preserve timeout, failure propagation, interrupt status, key mapping,
     vanilla flush, and close ownership behavior.
   - Keep the direct Spring Kafka declaration as `compileOnly`; dependency
     evidence may still show Spring Kafka transitively through
     `bluetape4k-kafka`.
   - Use `bluetape4k-kafka` `producerOf(...)` for vanilla producer factory
     overloads.

4. Refactor Kafka repositories.
   - `KafkaCdoSnapshotRepository.saveSnapshot()` builds a
     `CdoSnapshotEvent<String>` and delegates to `KafkaSnapshotEventPublisher`.
   - `VanillaKafkaCdoSnapshotRepository.saveSnapshot()` builds a
     `CdoSnapshotEvent<String>` and delegates to
     `VanillaKafkaSnapshotEventPublisher`.
   - Preserve write-only read path warnings and existing public constructor /
     companion factory ergonomics.

5. Add/update Kafka tests.
   - Add adapter-specific unit tests with class-level MockK fields reset by
     `@BeforeEach clearMocks`.
   - Keep existing repository behavior tests passing.
   - Add tests proving repository-created records still use global id key and
     encoded payload.

6. Update docs.
   - Update `javers-persistence-kafka/README.md`.
   - Update `javers-persistence-kafka/README.ko.md`.
   - Add transport selection table and NATS/SQS design artifacts.
   - Mention #105 and #131 boundaries.

7. Update review and lesson artifacts.
   - Add `docs/review/2026-06-08-issue-135-snapshot-event-pipeline-review.md`.
   - Add `docs/lessons/2026-06-08-issue-135-snapshot-event-pipeline.md`.

## Verification Tasks

1. CodeGraph or equivalent impact check before Kotlin edits where available.
2. Production pattern scan:
   - no `GlobalScope`, `runBlocking`, `Thread.sleep`, `synchronized`,
     `@Synchronized`, `runCatching`, `!!`, or UUID-derived suffixes in touched
     production/test files.
3. Targeted tests:
   - `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
4. Runtime dependency evidence:
   - `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|bluetape4k-nats|aws|sqs|kafka-clients"`
5. `git diff --check`.
6. Step 6-R 7-tier review with P0=0 and P1=0.
7. PR body created with `--body-file` and verified live; final section must be
   `## DoD Status`.

## Rejected Alternatives

- Add NATS implementation now: rejected because #135 can close the non-Kafka
  adapter requirement with a testable design artifact, and adding runtime
  dependencies would broaden CI and dependency governance.
- Add SQS implementation now: rejected because the repository catalog has no
  SQS/AWS SDK alias for this module.
- Make Kafka repositories read-capable: rejected because #105 owns read
  projection and #131 owns composite durable history plus event stream.
- Add asynchronous background buffering: rejected because current repository
  head semantics depend on publish failure propagation before `persist()` marks
  the commit head.

## Expected DoD

- Core event contract exists and is tested.
- Existing Kafka repository behavior is preserved through publisher adapters.
- NATS JetStream and SQS adapter semantics are documented as design artifacts.
- `bluetape4k-kafka` is used for Kafka helpers; no new NATS/SQS/AWS runtime
  dependency is introduced.
- Local tests, dependency check, diff check, and Step 6-R review pass.
