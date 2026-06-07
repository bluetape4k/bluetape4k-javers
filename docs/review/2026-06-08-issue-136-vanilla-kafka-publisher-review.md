# Issue #136 - Vanilla Kafka Snapshot Publisher Review

## Scope

Planned and implemented scope for issue #136:

- `docs/superpowers/specs/2026-06-08-issue-136-vanilla-kafka-publisher-design.md`
- `docs/superpowers/plans/2026-06-08-issue-136-vanilla-kafka-publisher-plan.md`
- `javers-persistence-kafka` production source, tests, and README locale pair
- `javers-persistence-kafka` Kafka test fixture IDs

This review file is updated as the branch advances through Step 2-R, Step 3-R,
and Step 6-R.

## Step 2-R Spec Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Kafka producer API, encoded payload, dependency boundary | No new deserialization or input trust boundary is introduced. The repository only publishes encoded JaVers JSON. The spec avoids mandatory `bluetape4k-kafka` runtime dependency because that artifact includes Spring support. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Timeout, failure propagation, lifecycle ownership | Spec requires positive `publishTimeout`, publish failure propagation, interrupt preservation, optional flush, and explicit close ownership. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module/API boundary | Public API is limited to a vanilla repository and options object inside the existing Kafka module. No new module or read projection behavior is introduced. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Constructor shape, validation, KDoc | Options object avoids same-typed boolean parameter ambiguity. KDoc and README are required in English/Korean as appropriate. | P0=0, P1=0, P2=0, P3=0 |
| 5 Testability | Failure, timeout, interruption, lifecycle, warning contract | Spec names concrete tests for every runtime behavior and the dependency boundary. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/Stability | Blocking send wait, flush behavior | Spec keeps blocking `Future.get(timeout)` bounded and makes `flushAfterSend` opt-in. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/Release | README locale pair, issue links, evidence | Spec requires README locale parity and records #105/#131 as non-goals. No CI/Nightly/module registration changes are needed. | P0=0, P1=0, P2=0, P3=0 |

Step 2-R verdict: PASS with P0=0 and P1=0.

## Step 3-R Plan Review

| Perspective | Finding | Required edit | Counts |
|---|---|---|---|
| Implementer | Tasks are ordered: evidence, spec/plan review, implementation, tests/docs, validation, final review, PR. | None. | P0=0, P1=0, P2=0, P3=0 |
| Test engineer | Plan maps each spec behavior to a named test and uses serial Kafka module verification. | None. | P0=0, P1=0, P2=0, P3=0 |
| Architect | Plan preserves Spring adapter, adds a vanilla adapter, and keeps read projection/composite behavior out of scope. | None. | P0=0, P1=0, P2=0, P3=0 |
| Delivery | Plan covers README locale pair, lesson, review artifact, dependency evidence, PR body verification, milestone, and assignee. | None. | P0=0, P1=0, P2=0, P3=0 |

Step 3-R verdict: PASS with P0=0 and P1=0.

## Step 6-R Final Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | `VanillaKafkaCdoSnapshotRepository`, options, README | No credential or auth surface added. Encoded JaVers JSON is published only; no deserialization or class-loading boundary is introduced. Runtime dependency evidence keeps Spring Kafka out of the vanilla production path. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Publish error paths, timeout, interruption, lifecycle | Publish is bounded by positive `publishTimeout`; failures propagate; `InterruptedException` restores interrupt status; producer close is explicit and opt-in. Tests cover each behavior. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module boundary and API compatibility | Existing `KafkaCdoSnapshotRepository` remains unchanged. New class accepts Apache `Producer` directly and does not add mandatory `bluetape4k-kafka` or Spring runtime dependencies. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin quality | Production Kotlin and tests | Public API KDoc is English; options data class is `Serializable`; topic validation uses bluetape4k `requireNotBlank`; timeout uses standard `require` because no matching helper exists; constructor access now follows companion `invoke`; MockK producer/metadata mocks are class fields reset by `@BeforeEach clearMocks`; Kafka test fixture unique IDs use `Base58.randomString`; no `!!`, `runBlocking`, `GlobalScope`, synchronization, or `runCatching` hit in production scan. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | `VanillaKafkaCdoSnapshotRepositoryTest`, existing Kafka tests | Tests cover success payload, custom key mapping, failure propagation, timeout, interruption, flush, close ownership, validation, head rebuild behavior, and write-only warning parity. Targeted module tests executed 18 tests. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Blocking send wait, flush, close, tests | Blocking wait is bounded by caller-configured timeout. `flushAfterSend` defaults false and is tested as opt-in. No unbounded polling, retries, buffers, or coroutine cancellation surface introduced. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | README locale pair, spec/plan/review evidence | README English/Korean pair updated with adapter selection and optional `bluetape4k-kafka` helper boundary. No module registration, CI, Nightly, BOM, or changelog update needed. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R verdict: PASS with P0=0 and P1=0.

## Post-PR Review Comment Follow-up

- Thread `PRRT_kwDOSVj8-s6Hqnle`: addressed by making `VanillaKafkaCdoSnapshotRepository` constructor private and exposing a companion `operator fun invoke(...)` factory.
- Thread `PRRT_kwDOSVj8-s6Hqn90`: addressed by moving producer and metadata MockK instances to class fields and clearing them in `@BeforeEach`.
- Additional `bluetape4k-code-patterns` sweep: replaced Kafka test fixture `UUID.randomUUID().encodeUrl62()` IDs with `Base58.randomString(8)` suffixes.
- Follow-up verification: `:javers-persistence-kafka:test` PASS, 18 tests executed; `git diff --check` PASS.

## Validation Evidence

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 18 tests executed.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "spring-kafka|bluetape4k-kafka|kafka-clients"`
  - Result: only `org.apache.kafka:kafka-clients:4.2.0` appeared; no `spring-kafka` or `bluetape4k-kafka` appeared in production runtime classpath.
- `git diff --check`
  - Result: PASS, no whitespace errors.

## Final Gate

P0=0. P1=0. PR creation is allowed.
