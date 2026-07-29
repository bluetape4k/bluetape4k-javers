# Issue #176 - Spring Kafka Timeout Validation Review

## 범위

Issue #176은 Spring Kafka snapshot publisher path와 vanilla Kafka path 사이의
asymmetric timeout validation을 수정한다.

검토한 파일:

- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaPublishTimeoutSupport.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryTest.kt`
- `docs/lessons/2026-06-08-issue-176-spring-kafka-timeout-validation.md`

## Step 6-R Final Review

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Kafka publish input boundary | 변경은 publish 전에 invalid timeout value를 reject한다. deserialization, credential, external input expansion은 추가하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Bounded publish wait | `requirePositivePublishTimeout()`은 bluetape4k-core `requireGt(Duration.ZERO, ...)`로 delegate하여 zero/negative duration을 reject하고 Spring/vanilla Kafka bounded-wait semantics를 맞춘다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Adapter boundary | helper는 package-internal이며 Spring repository, Spring publisher, vanilla options에서 재사용된다. public API shape 또는 module dependency 변경은 없다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Validation과 constructor behavior | Public construction boundary는 이제 fail fast한다. `KafkaSnapshotEventPublisher`는 `init` validation 대신 private constructor와 companion `invoke`를 사용한다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Regression tests | 테스트는 `KafkaCdoSnapshotRepository`와 `KafkaSnapshotEventPublisher` 양쪽의 `Duration.ZERO` 및 negative duration을 커버한다. 기존 vanilla non-positive timeout test도 shared helper를 통해 계속 통과한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Runtime behavior | valid positive timeout은 기존 blocking `Future.get(timeout)` behavior를 보존한다. invalid value는 이제 publish time이 아니라 construction에서 fail한다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | Lesson 및 release impact | Lesson은 adapter validation rule을 기록한다. 이 internal consistency fix에는 README, CI, Nightly, BOM, changelog update가 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R 판정: P0=0, P1=0으로 PASS.

## 증거

- `KafkaPublishTimeoutSupport.kt:5-7`: shared positive timeout guard delegates to bluetape4k-core `requireGt`.
- `KafkaCdoSnapshotRepository.kt:53`: Spring repository validates before constructing the publisher.
- `KafkaSnapshotEventPublisher.kt:25-39`: companion `invoke` validates direct publisher construction independently.
- `VanillaKafkaCdoSnapshotRepository.kt:54-55`: vanilla options use the same guard.
- `KafkaCdoSnapshotRepositoryTest.kt:195-227`: Spring repository and publisher regression tests cover zero and negative durations.

## 검증 증거

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 24 tests executed.
- `git diff --check`
  - 결과: PASS, no whitespace errors.

## Final Gate 판정

P0=0. P1=0. PR creation is allowed.
