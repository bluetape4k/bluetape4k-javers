# Issue #176 - Spring Kafka Timeout Validation Review

## Scope

Issue #176 fixes asymmetric timeout validation between the Spring Kafka snapshot
publisher path and the vanilla Kafka path.

Reviewed files:

- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaPublishTimeoutSupport.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt`
- `docs/lessons/2026-06-08-issue-176-spring-kafka-timeout-validation.md`

## Step 6-R Final Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Kafka publish input boundary | The change rejects invalid timeout values before publish. It does not add deserialization, credential, or external input expansion. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Bounded publish wait | `requirePositivePublishTimeout()` delegates to bluetape4k-core `requireGt(Duration.ZERO, ...)`, rejecting zero and negative durations while keeping Spring and vanilla Kafka bounded-wait semantics aligned. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Adapter boundary | The helper is package-internal and reused by Spring repository, Spring publisher, and vanilla options. No public API shape or module dependency changes. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Validation and constructor behavior | Public construction boundaries now fail fast. `KafkaSnapshotEventPublisher` uses a private constructor plus companion `invoke` instead of `init` validation. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Regression tests | Tests cover `Duration.ZERO` and negative duration for both `KafkaCdoSnapshotRepository` and `KafkaSnapshotEventPublisher`. Existing vanilla non-positive timeout test still passes through the shared helper. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Runtime behavior | Valid positive timeouts preserve existing blocking `Future.get(timeout)` behavior. Invalid values now fail at construction instead of at publish time. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | Lessons and release impact | Lessons capture the adapter validation rule. No README, CI, Nightly, BOM, or changelog update is needed for this internal consistency fix. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R verdict: PASS with P0=0 and P1=0.

## Evidence

- `KafkaPublishTimeoutSupport.kt:5-7`: shared positive timeout guard delegates to bluetape4k-core `requireGt`.
- `KafkaCdoSnapshotRepository.kt:53`: Spring repository validates before constructing the publisher.
- `KafkaSnapshotEventPublisher.kt:25-39`: companion `invoke` validates direct publisher construction independently.
- `VanillaKafkaCdoSnapshotRepository.kt:54-55`: vanilla options use the same guard.
- `KafkaCdoSnapshotRepositoryTest.kt:195-227`: Spring repository and publisher regression tests cover zero and negative durations.

## Validation Evidence

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 24 tests executed.
- `git diff --check`
  - Result: PASS, no whitespace errors.

## Final Gate

P0=0. P1=0. PR creation is allowed.
