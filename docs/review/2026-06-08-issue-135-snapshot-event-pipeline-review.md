# Issue #135 - Snapshot Event Pipeline Review

## Scope

- `docs/superpowers/specs/2026-06-08-issue-135-snapshot-event-pipeline-design.md`
- `docs/superpowers/plans/2026-06-08-issue-135-snapshot-event-pipeline-plan.md`
- Planned `javers-core` event contract
- Planned `javers-persistence-kafka` publisher adapters and README locale pair

## Step 2-R Spec Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Event metadata and payload | No new deserialization, credential, or auth boundary is introduced. Metadata is derived from trusted JaVers snapshot objects. SQS/NATS remain design artifacts, avoiding unreviewed client configuration. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Publish acknowledgement and failures | Spec preserves synchronous acknowledgement and failure propagation, so repository head does not advance on publish failure. Interrupt preservation is required. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Core API and Kafka module boundary | Core contract is transport-neutral and dependency-free. Kafka adapters stay in the Kafka module and use the governed `bluetape4k-kafka` helper dependency. #105 and #131 remain out of scope. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Public API shape | Serializable value objects, a fun-interface publisher, English KDoc, and explicit nullable repository sequence keep the API idiomatic and hard to misuse. | P0=0, P1=0, P2=0, P3=0 |
| 5 Testability | Metadata and adapter behavior | Spec names concrete unit tests for metadata, idempotency, adapter delegation, timeout/failure, interrupt, flush, and close ownership. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/Stability | Blocking and buffering | No background queue, retry loop, or batching is introduced. Kafka publish waits remain bounded by configured timeout. | P0=0, P1=0, P2=0, P3=0 |
| 7 Docs/Release | README and dependency governance | README locale pair and design artifacts are required. No module registration, CI/Nightly, or BOM change is needed because `bluetape4k-kafka` is already governed by the catalog. | P0=0, P1=0, P2=0, P3=0 |

Step 2-R verdict: PASS with P0=0 and P1=0.

## Step 3-R Plan Review

| Perspective | Finding | Required edit | Counts |
|---|---|---|---|
| Implementer | Tasks are ordered from core contract to adapters, repository refactor, tests, docs, and review. | None. | P0=0, P1=0, P2=0, P3=0 |
| Test engineer | Plan maps every behavior to targeted core/Kafka tests and names the serial Gradle command. | None. | P0=0, P1=0, P2=0, P3=0 |
| Architect | Plan keeps dependency-free contract in core and transport-specific code in Kafka module, with `bluetape4k-kafka` limited to the Kafka module. | None. | P0=0, P1=0, P2=0, P3=0 |
| Delivery | Plan covers README English/Korean, review artifact, lesson, dependency evidence, PR body rule, and CI gate. | None. | P0=0, P1=0, P2=0, P3=0 |

Step 3-R verdict: PASS with P0=0 and P1=0.

## Step 6-R Final Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Event payload and metadata | No new deserialization path, credential handling, or auth boundary was added. Publishers forward already encoded JaVers JSON strings and propagate transport failures. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness | Repository head and failure semantics | Both Kafka repositories build `CdoSnapshotEvent<String>` and delegate to synchronous publishers. Publish failures still abort `persist()` before head advancement. Interrupt status is restored. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Dependency and module boundaries | Core event API stays in `javers-core` and has no Kafka dependency. Kafka adapters stay in `javers-persistence-kafka`; vanilla producer factory uses governed `bluetape4k-kafka`. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Data class validation and KDoc | Constructor validation follows the companion `invoke` pattern with private constructors and `@ConsistentCopyVisibility`. Public APIs have English KDoc. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests | Core and Kafka behavior | Core metadata tests cover snapshot extraction, validation, and event payload. Existing Spring/vanilla Kafka repository tests pass through the publisher adapters. | P0=0, P1=0, P2=0, P3=0 |
| 6 Docs | README locale pair and design artifacts | English/Korean README files describe the event contract, Kafka adapter selection, and planned NATS/SQS boundaries. | P0=0, P1=0, P2=0, P3=0 |
| 7 Delivery | Issue and workflow evidence | The plan, spec, and issue body were refreshed for the `bluetape4k-kafka` scope. No new module, CI, Nightly, or BOM registration is required. | P0=0, P1=0, P2=0, P3=0 |

Final verdict: PASS with P0=0 and P1=0.

## Strict Re-Review Follow-up

Initial review was too implementation-centric. A stricter pass found and fixed:

| Severity | Location | Finding | Fix |
|---|---|---|---|
| P2 | `javers-persistence-kafka/build.gradle.kts` | `bluetape4k-kafka` was used as an internal helper but declared as `api`, which would leak its compile classpath to consumers. | Changed it to `implementation(libs.bluetape4k.kafka)` and verified `api` dependencies do not include Kafka helper dependencies. |
| P2 | `KafkaCdoSnapshotRepository.saveSnapshot`, `VanillaKafkaCdoSnapshotRepository.saveSnapshot` | Trace logs included the full encoded snapshot payload, which can contain audit data and user fields. | Reduced trace logs to key, snapshot version, and codec metadata only. |
| P2 | Kafka tests | New public helper path and Spring interrupt behavior were under-tested. | Added a repository-created producer test through `bluetape4k-kafka` `producerOf(...)` and a Spring Kafka interrupt-preservation test. |
| P3 | PR review comments | Explicit publisher key parameters did not guard blank values. | Added `key.requireNotBlank("key")` to Spring and vanilla publisher explicit-key paths with blank-key regression tests. |

Strict re-review verdict after fixes: PASS with P0=0 and P1=0.

## Verification Evidence

- `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: BUILD SUCCESSFUL; `javers-core` executed 184 tests; `javers-persistence-kafka` executed 20 tests.
- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: BUILD SUCCESSFUL after PR comment fixes; `javers-persistence-kafka` executed 22 tests.
- `./gradlew :javers-persistence-kafka:dependencies --configuration api --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|kafka-clients|No dependencies|io.github.bluetape4k|org.apache.kafka"`
  - Result: `api` dependencies include the intended bluetape4k API modules only; no `bluetape4k-kafka`, `spring-kafka`, or `kafka-clients` API leak appeared.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|bluetape4k-nats|aws|sqs|kafka-clients"`
  - Result: `bluetape4k-kafka` and `kafka-clients` are present. No `bluetape4k-nats`, AWS, or SQS runtime dependency appeared. `spring-kafka` appears transitively through `bluetape4k-kafka`; the direct module declaration remains `compileOnly`.
- `rg -n "GlobalScope|runBlocking|Thread\\.sleep|synchronized\\s*\\(|@Synchronized|runCatching|!!|UUID" ...`
  - Result: no matches in touched source/test paths.
- `git diff --check`
  - Result: PASS.
