# Issue 192 Release-Prep 7-Tier Review

Snapshot: 2026-06-26 KST
Scope: `0.3.0` release-prep line을 위한 `develop` repository-wide review.

## 판정

P0 gate: PASS. critical correctness, data-loss, credential, publication stopper는
발견하지 않았다.

P1 gate: FAIL. 다섯 개 release-prep blocker를 확인했고 follow-up issue로 추적한다.

| Severity | Issue | 결과 |
|---|---|---|
| P1 | [#208](https://github.com/bluetape4k/bluetape4k-javers/issues/208) | DDD aggregate save는 하나의 consistency boundary 없이 source data를 persist한 뒤 JaVers commit을 수행하고 event를 publish한다. |
| P1 | [#209](https://github.com/bluetape4k/bluetape4k-javers/issues/209) | Durable snapshot repository는 `persist()`가 snapshot을 하나씩 쓰기 때문에 partial multi-snapshot commit을 노출할 수 있다. |
| P1 | [#211](https://github.com/bluetape4k/bluetape4k-javers/issues/211) | Kafka projection replay는 snapshot을 직접 쓰며 repository head/sequence restoration을 우회한다. |
| P1 | [#212](https://github.com/bluetape4k/bluetape4k-javers/issues/212) | Published BOM이 non-published example 및 benchmark module을 constrain할 수 있다. |
| P1 | [#213](https://github.com/bluetape4k/bluetape4k-javers/issues/213) | repository license는 MIT인데 published POM license metadata는 Apache-2.0이라고 말한다. |

Release recommendation: #208, #209, #211, #212, #213이 해결되거나 documented
semantics와 함께 명시적으로 accept되기 전에는 `0.3.0` tag를 만들지 않는다. 가장 빠른
safe first lane은 #213, 그다음 #212다. 둘 다 low-effort release metadata blocker이기
때문이다.

## Review Evidence

Review source:

- core, DDD, Exposed, Redis, Kafka, Spring Boot autoconfigure, examples,
  benchmark, workflows, publication metadata의 local source inspection.
- Independent code-review pass는 P0 finding 없음, P1 data-integrity finding 2개,
  P2 lifecycle finding 1개, P3 diagnostic-output finding 1개를 보고했다.
- Independent architecture pass는 `WATCH` / `REQUEST CHANGES`를 보고하면서 Kafka
  replay, BOM, license metadata P1 finding을 추가했다.
- reviewed tree에서 `git diff --check`가 통과했다.
- Independent reviewer verification:
  `./gradlew compileTestKotlin --warning-mode all --continue --no-configuration-cache --rerun-tasks`
  passed (`37 actionable tasks: 37 executed`).

## P1 Findings

### #208 DDD Aggregate Save Consistency

`AggregateRepository.save(...)` has three sequential effects:
`persist(aggregate)`, `javers.commit(...)`, and `eventPublisher.publishAll(...)`
in `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt:46-50`.

Exposed example repository는 JaVers commit/event publication이 실행되기 전에 자체
transaction 안에서 source-of-truth row를 commit한다.

- `examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/persistence/OrderRepository.kt:36-64`
- `examples/javers-spring-boot4/src/main/kotlin/io/bluetape4k/javers/examples/springboot4/persistence/OrderRepository.kt:36-64`
- `examples/javers-ktor/src/main/kotlin/io/bluetape4k/javers/examples/ktor/persistence/OrderRepository.kt:36-64`

Risk: source persistence 이후 JaVers commit 또는 event publication이 실패하면 source
state가 audit/event state와 diverge할 수 있다.

### #209 Durable Commit Atomicity

`AbstractCdoSnapshotRepository.persist(...)`는 snapshot을 각각 쓰고 loop 이후에만
head/sequence를 advance한다
(`javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt:204-218`).

Durable repository는 backend boundary마다 snapshot 하나를 저장한다.

- Exposed: `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt:178-193`
- Lettuce: `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt:125-140`
- Redisson: `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt:107-112`

Risk: head/sequence metadata가 advance되기 전에 later snapshot write가 실패하면
multi-snapshot commit이 earlier snapshot을 visible 상태로 남길 수 있다.

### #211 Kafka Projection Replay Semantics

`KafkaCdoSnapshotProjector.projectOnce()` decodes records and calls
`projectionRepository.saveSnapshot(snapshot)` directly
(`javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/projection/KafkaCdoSnapshotProjector.kt:130-158`).

이 방식은 `head`와 commit sequence metadata를 update하는 base `persist()` path를
우회한다. Exposed와 Redis repository는 별도 path에서 sequence metadata로 head를
restore한다.

- `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt:204-231`
- `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt:151-168`
- `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt:93-117`

Risk: replay가 snapshot row를 rebuild하는 동안 `getHeadId()` / ordering semantics가
incomplete 또는 stale 상태로 남을 수 있다.

### #212 BOM Publishable Surface

Root build는 `isExampleProject()`를 통해 example과 benchmark를 NMCP aggregation에서
제외한다.

- `build.gradle.kts:40-46`
- `build.gradle.kts:341-345`

`settings.gradle.kts:63-76`은 여전히 example과 benchmark module을 normal subproject로
등록하고, `bom/build.gradle.kts:7-15`는 BOM 자체를 제외한 모든 subproject를 constrain한다.

Risk: released BOM이 non-published example/benchmark module을 consumer에게 advertise할 수 있다.

### #213 POM License Metadata

Repository license는 MIT다.

- `LICENSE:1`
- `README.md:6`

Published module과 BOM POM metadata는 Apache-2.0을 declare한다.

- `build.gradle.kts:294-303`
- `bom/build.gradle.kts:22-31`

Risk: Maven Central artifact가 repository license와 충돌하는 license metadata를 ship할 수 있다.

## P2 / P3 Follow-Ups

| Severity | Issue | 결과 |
|---|---|---|
| P2 | [#210](https://github.com/bluetape4k/bluetape4k-javers/issues/210) | Lettuce repository는 lazy command handle을 만들지만 explicit lifecycle/close contract가 없다. |
| P2 | [#118](https://github.com/bluetape4k/bluetape4k-javers/issues/118) | Envers comparison benchmark는 아직 example test에서 benchmark module로 이동해야 한다. |
| P2 | [#195](https://github.com/bluetape4k/bluetape4k-javers/issues/195) | Benchmark module에는 README와 intentional smoke coverage가 필요하다. |
| P3 | [#193](https://github.com/bluetape4k/bluetape4k-javers/issues/193) | 남은 data-class validation factory는 current code-pattern guidance와 맞춰야 한다. |
| P3 | [#194](https://github.com/bluetape4k/bluetape4k-javers/issues/194) | current source가 이미 `SchemaUtils.create(...)`를 사용하지만 Spring example schema initialization은 정리해야 한다. |

Additional watch item: `ConsoleDispatcher`는
`javers-core/src/main/kotlin/io/bluetape4k/javers/dispatcher/internal/ConsoleDispatcher.kt:10`에서
full domain object를 stdout에 출력한다. priority는 낮지만 production-safe logging으로
표현하면 안 된다.

## 긍정적 결과

- Spring Boot auto-configuration은 structural하게 clean하다. backend phase class는
  `AutoConfiguration.imports`에 직접 나열되고 class/bean/property condition으로
  guard되며, default `Javers` bean은 repository bean을 요구한다.
- Exposed schema creation은 이미 deprecated
  `createMissingTablesAndColumns(...)` production path를 피한다. repository는
  `ExposedCdoSnapshotRepository.kt:117-124`에서 `SchemaUtils.create(*schema.tables)`를 사용한다.
- Kafka/Spring publisher path는 `KafkaSnapshotEventPublisher.kt:77-83` 및
  `VanillaKafkaSnapshotEventPublisher.kt:39-48`에서 propagation 전에 thread interrupt
  status를 restore한다.
- In-memory Caffeine 및 JCache repository는 explicit `ReentrantLock` 사용으로
  mutable snapshot list를 guard한다.

## Closure Criteria

이 review artifact가 merge되고 follow-up issue set이 release-prep queue로 accept된 뒤
#192를 닫는다. P1 follow-up이 수정되거나 documented release semantics와 함께 명시적으로
accept된 뒤에만 `0.3.0` release gate를 닫는다.
