# Issue 104 7-Tier Code Review

## Reviewed Scope

- New module: `javers-spring-boot4-autoconfigure`
- Kafka publisher contract changes in `javers-persistence-kafka`
- Gradle registration: `settings.gradle.kts`
- Public docs: root/module/BOM `README.md`, `README.ko.md`, `CHANGELOG.md`
- Repo guidance: `AGENTS.md`
- CI/Nightly module coverage: `.github/workflows/ci.yml`,
  `.github/workflows/nightly-tests.yml`

## Independent Review Lanes

The current native callable surface did not expose OMX `agent_type`, so the
review used bounded role-injected subagent lanes instead of skipping
independent work.

| Lane | Role | Result |
|---|---|---|
| Anscombe | `code-reviewer` | Found Spring Kafka topic binding and test assertion quality issues. |
| Bernoulli | `architect` | Found P1 Spring Kafka ordering risk with Boot-created `KafkaTemplate`; also flagged topic binding and Exposed DDL defaults. |
| Huygens | `verifier` | Found BOM README omission and stale PR evidence wording. |

All P1/P2/P3 findings were fixed before this final gate.

## Findings After Fixes

| Tier | Focus | P0 | P1 | P2 | P3 | Verdict |
|---|---|---:|---:|---:|---:|---|
| 1. Security | Secrets, trust boundaries, payload exposure | 0 | 0 | 0 | 0 | PASS |
| 2. Ops/SRE reliability | Startup ordering, DDL defaults, Kafka publish failure | 0 | 0 | 0 | 0 | PASS |
| 3. Structural impact | Module boundary, compileOnly guards, Boot 4 modularization | 0 | 0 | 0 | 0 | PASS |
| 4. Kotlin code quality | Validation helpers, public API KDoc, assertion style | 0 | 0 | 0 | 0 | PASS |
| 5. Tests/types/silent failure | Boot-created bean ordering, configured Kafka topic, schema defaults | 0 | 0 | 0 | 0 | PASS |
| 6. Performance/stability | Bounded Kafka send, opt-in DDL, no new background work | 0 | 0 | 0 | 0 | PASS |
| 7. Documentation/release/evidence | README locale parity, BOM docs, PR body evidence | 0 | 0 | 0 | 0 | PASS |

Final gate: `P0 = 0`, `P1 = 0`.

## Fixed Review Findings

| Severity | Finding | Fix |
|---|---|---|
| P1 | `JaversSpringKafkaRepositoryAutoConfiguration` could evaluate before Spring Boot 4 Kafka auto-configuration creates `KafkaTemplate`, silently backing off for `repository.type=spring-kafka`. | Added `afterName = ["org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"]`, added `spring-boot-kafka` compile/test dependency, and covered Boot-created `KafkaTemplate` with `ApplicationContextRunner`. |
| P2 | `bluetape4k.javers.kafka.topic` was ignored by the Spring Kafka repository path. | Added optional `topic` to `KafkaCdoSnapshotRepository` and `KafkaSnapshotEventPublisher`; auto-config now passes the validated topic. |
| P2 | Exposed auto-config performed DDL by default through `initializeSchema=true` and `createSchemaOnEnsure=true`. | Changed both defaults to `false`, preserving explicit opt-in behavior. |
| P2 | BOM README locale pair omitted the new module. | Added `bluetape4k-javers-spring-boot4-autoconfigure` to both BOM README files. |
| P1 | Adding `topic` before `keyMapper` in `KafkaSnapshotEventPublisher.invoke(...)` broke source compatibility for trailing-lambda callers. | Restored the existing companion `invoke(kafkaOperations, publishTimeout, keyMapper)` signature and added `withTopic(...)` for explicit-topic publishing. |
| P2 | Adding `topic` to the public `KafkaCdoSnapshotRepository` primary constructor risked binary/source compatibility. | Restored the old primary constructor and added a secondary topic constructor; `javap` confirmed the old `(KafkaTemplate, Duration)` and default-mask constructor descriptors remain present. |
| P3 | `spring-boot-kafka` was on the production compile classpath even though production code only uses an `afterName` string. | Removed production `compileOnly("org.springframework.boot:spring-boot-kafka")`; kept test dependency for Boot 4 class verification. |
| P3 | Type checks used boolean `is` assertions. | Switched touched tests to `shouldBeInstanceOf`. |
| P3 | Bean-presence assertions used `shouldBeEqualTo true/false`. | Switched touched tests to `shouldBeTrue()` / `shouldBeFalse()`. |
| P3 | PR/review evidence wording was stale after CI/test completion. | Updated tracked review evidence and PR body will be refreshed from current validation. |

## Evidence

| Evidence | Result |
|---|---|
| Context7 Spring Boot 4 docs query for Kafka auto-configuration | Confirmed Spring Boot 4 Kafka auto-config class is `org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration` in `spring-boot-kafka`. |
| `gno query "bluetape4k-javers module registration CI Nightly coverage AutoConfiguration" -c bluetape4k-github --fast --no-rerank` | Found prior Spring Boot 4 example/module-registration PR evidence. |
| `gno query "bluetape4k-javers module registration CI Nightly coverage AutoConfiguration" -c bluetape4k-docs --no-rerank` | Found issue #140 Spring Boot 4 example research/review docs. |
| `rg "GlobalScope\|runBlocking\(\|Thread\.sleep\|delay\(\|synchronized\(\|@Synchronized\|runCatching\s*\{" javers-spring-boot4-autoconfigure/src/main/kotlin javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt` | Zero risky concurrency hits in touched production sources. |
| `rg "assertThat\|assertThrows\|kotlin\.test\|Assertions\." javers-spring-boot4-autoconfigure/src/test/kotlin javers-persistence-kafka/src/test/kotlin` | Zero forbidden assertion API hits in touched test sources. |
| `./gradlew :javers-persistence-kafka:compileKotlin :javers-persistence-kafka:compileTestKotlin :javers-spring-boot4-autoconfigure:compileKotlin :javers-spring-boot4-autoconfigure:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS. |
| `javap -classpath javers-persistence-kafka/build/classes/kotlin/main io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository` | PASS; old public `(KafkaTemplate, Duration)` constructor and Kotlin default-mask constructor remain present, with a new topic constructor added. |
| `javap -classpath javers-persistence-kafka/build/classes/kotlin/main io.bluetape4k.javers.persistence.kafka.repository.KafkaSnapshotEventPublisher\\$Companion` | PASS; old `invoke(KafkaTemplate, Duration, Function1)` and `invoke$default` remain present, with `withTopic(...)` added. |
| `./gradlew :javers-spring-boot4-autoconfigure:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS; 17 tests executed, including Boot-created `KafkaTemplate` ordering and configured topic publishing. |
| `./gradlew :javers-persistence-kafka:cleanTest :javers-spring-boot4-autoconfigure:cleanTest :javers-persistence-kafka:test :javers-spring-boot4-autoconfigure:test :javers-spring-boot4-autoconfigure:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS; Kafka executed 41 tests, auto-config executed 17 tests, and Kover XML was generated. |
| `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` | PASS. |
| `git diff --check` | PASS. |

## Notes

- `org.assertj:assertj-core` is present only because Spring Boot
  `ApplicationContextRunner` exposes AssertJ-based API types at compile time.
  Touched tests use bluetape4k assertions.
- `spring-boot-kafka` is used as a compile/test boundary for Boot 4
  auto-configuration metadata and class-name ordering; the module still does not
  create application-owned Kafka infrastructure beans.
