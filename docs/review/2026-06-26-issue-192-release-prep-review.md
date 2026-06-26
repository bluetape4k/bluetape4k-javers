# Issue 192 Release-Prep 7-Tier Review

Snapshot: 2026-06-26 KST
Scope: repository-wide review of `develop` for the `0.3.0` release-prep line.

## Verdict

P0 gate: PASS. No critical correctness, data-loss, credential, or publication
stopper was found.

P1 gate: FAIL. Five release-prep blockers were confirmed and tracked as
follow-up issues:

| Severity | Issue | Finding |
|---|---|---|
| P1 | [#208](https://github.com/bluetape4k/bluetape4k-javers/issues/208) | DDD aggregate save persists source data, then commits JaVers, then publishes events without one consistency boundary. |
| P1 | [#209](https://github.com/bluetape4k/bluetape4k-javers/issues/209) | Durable snapshot repositories can expose partial multi-snapshot commits because `persist()` writes snapshots one by one. |
| P1 | [#211](https://github.com/bluetape4k/bluetape4k-javers/issues/211) | Kafka projection replay writes snapshots directly and bypasses repository head/sequence restoration. |
| P1 | [#212](https://github.com/bluetape4k/bluetape4k-javers/issues/212) | The published BOM can constrain non-published example and benchmark modules. |
| P1 | [#213](https://github.com/bluetape4k/bluetape4k-javers/issues/213) | Published POM license metadata says Apache-2.0 while the repository license is MIT. |

Release recommendation: do not tag `0.3.0` until #208, #209, #211, #212, and
#213 are resolved or explicitly accepted with documented semantics. The fastest
safe first lane is #213, then #212, because both are low-effort release metadata
blockers.

## Review Evidence

Review sources:

- Local source inspection of core, DDD, Exposed, Redis, Kafka, Spring Boot
  autoconfigure, examples, benchmark, workflows, and publication metadata.
- Independent code-review pass reported no P0 findings, two P1 data-integrity
  findings, one P2 lifecycle finding, and one P3 diagnostic-output finding.
- Independent architecture pass reported `WATCH` / `REQUEST CHANGES`, adding
  Kafka replay, BOM, and license metadata P1 findings.
- `git diff --check` passed on the reviewed tree.
- Independent reviewer verification: `./gradlew compileTestKotlin --warning-mode all --continue --no-configuration-cache --rerun-tasks`
  passed (`37 actionable tasks: 37 executed`).

## P1 Findings

### #208 DDD Aggregate Save Consistency

`AggregateRepository.save(...)` has three sequential effects:
`persist(aggregate)`, `javers.commit(...)`, and `eventPublisher.publishAll(...)`
in `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt:46-50`.

The Exposed example repositories commit source-of-truth rows inside their own
transactions before the JaVers commit/event publication runs:

- `examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/persistence/OrderRepository.kt:36-64`
- `examples/javers-spring-boot4/src/main/kotlin/io/bluetape4k/javers/examples/springboot4/persistence/OrderRepository.kt:36-64`
- `examples/javers-ktor/src/main/kotlin/io/bluetape4k/javers/examples/ktor/persistence/OrderRepository.kt:36-64`

Risk: if JaVers commit or event publication fails after source persistence,
source state can diverge from audit/event state.

### #209 Durable Commit Atomicity

`AbstractCdoSnapshotRepository.persist(...)` writes each snapshot and advances
head/sequence only after the loop
(`javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt:204-218`).

Durable repositories save one snapshot per backend boundary:

- Exposed: `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt:178-193`
- Lettuce: `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt:125-140`
- Redisson: `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt:107-112`

Risk: a multi-snapshot commit can leave earlier snapshots visible if a later
snapshot write fails before head/sequence metadata is advanced.

### #211 Kafka Projection Replay Semantics

`KafkaCdoSnapshotProjector.projectOnce()` decodes records and calls
`projectionRepository.saveSnapshot(snapshot)` directly
(`javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/projection/KafkaCdoSnapshotProjector.kt:130-158`).

That bypasses the base `persist()` path that updates `head` and commit sequence
metadata. Exposed and Redis repositories restore head from sequence metadata in
separate paths:

- `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt:204-231`
- `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt:151-168`
- `javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt:93-117`

Risk: replay can rebuild snapshot rows while `getHeadId()` / ordering semantics
remain incomplete or stale.

### #212 BOM Publishable Surface

The root build excludes examples and benchmarks from NMCP aggregation through
`isExampleProject()`:

- `build.gradle.kts:40-46`
- `build.gradle.kts:341-345`

`settings.gradle.kts:63-76` still registers examples and the benchmark module as
normal subprojects, and `bom/build.gradle.kts:7-15` constrains every subproject
except the BOM itself.

Risk: the released BOM can advertise non-published example/benchmark modules to
consumers.

### #213 POM License Metadata

The repository license is MIT:

- `LICENSE:1`
- `README.md:6`

The published module and BOM POM metadata declare Apache-2.0:

- `build.gradle.kts:294-303`
- `bom/build.gradle.kts:22-31`

Risk: Maven Central artifacts would ship license metadata that conflicts with
the repository license.

## P2 / P3 Follow-Ups

| Severity | Issue | Finding |
|---|---|---|
| P2 | [#210](https://github.com/bluetape4k/bluetape4k-javers/issues/210) | Lettuce repository creates lazy command handles but has no explicit lifecycle/close contract. |
| P2 | [#118](https://github.com/bluetape4k/bluetape4k-javers/issues/118) | Envers comparison benchmark still needs to move out of example tests into a benchmark module. |
| P2 | [#195](https://github.com/bluetape4k/bluetape4k-javers/issues/195) | Benchmark module needs README and intentional smoke coverage. |
| P3 | [#193](https://github.com/bluetape4k/bluetape4k-javers/issues/193) | Remaining data-class validation factories should align with current code-pattern guidance. |
| P3 | [#194](https://github.com/bluetape4k/bluetape4k-javers/issues/194) | Spring example schema initialization should be cleaned up, although current source already uses `SchemaUtils.create(...)`. |

Additional watch item: `ConsoleDispatcher` prints full domain objects to stdout
in `javers-core/src/main/kotlin/io/bluetape4k/javers/dispatcher/internal/ConsoleDispatcher.kt:10`.
This is low priority but should not be represented as production-safe logging.

## Positive Findings

- Spring Boot auto-configuration is structurally clean: backend phase classes are
  directly listed in `AutoConfiguration.imports`, guarded by class/bean/property
  conditions, and the default `Javers` bean requires a repository bean.
- Exposed schema creation already avoids the deprecated
  `createMissingTablesAndColumns(...)` production path; the repository uses
  `SchemaUtils.create(*schema.tables)` in `ExposedCdoSnapshotRepository.kt:117-124`.
- Kafka/Spring publisher paths restore thread interrupt status before
  propagation in `KafkaSnapshotEventPublisher.kt:77-83` and
  `VanillaKafkaSnapshotEventPublisher.kt:39-48`.
- In-memory Caffeine and JCache repositories guard mutable snapshot lists with
  explicit `ReentrantLock` usage.

## Closure Criteria

Close #192 after this review artifact is merged and the follow-up issue set is
accepted as the release-prep queue. Close the `0.3.0` release gate only after the
P1 follow-ups are fixed or explicitly accepted with documented release semantics.
