# Issue 104 7-Tier Code 검토

## 검토 범위

- 새 module: `javers-spring-boot4-autoconfigure`
- `javers-persistence-kafka`의 Kafka publisher contract 변경
- Gradle registration: `settings.gradle.kts`
- Public docs: root/module/BOM `README.md`, `README.ko.md`, `CHANGELOG.md`
- Repo guidance: `AGENTS.md`
- CI/Nightly module coverage: `.github/workflows/ci.yml`,
  `.github/workflows/nightly-tests.yml`

## 독립 검토 lane

당시 native callable surface는 OMX `agent_type`을 노출하지 않았다. 따라서 review는
independent work를 생략하지 않고 bounded role-injected subagent lane을 사용했다.

| Lane | Role | 결과 |
|---|---|---|
| Anscombe | `code-reviewer` | Spring Kafka topic binding과 test assertion quality issue를 발견했다. |
| Bernoulli | `architect` | Boot-created `KafkaTemplate`의 P1 Spring Kafka ordering risk를 발견했고 topic binding 및 Exposed DDL default도 지적했다. |
| Huygens | `verifier` | BOM README omission과 stale PR evidence wording을 발견했다. |

모든 P1/P2/P3 finding은 이 final gate 전에 수정했다.

## 수정 후 결과

| Tier | 초점 | P0 | P1 | P2 | P3 | 판정 |
|---|---|---:|---:|---:|---:|---|
| 1. Security | Secrets, trust boundaries, payload exposure | 0 | 0 | 0 | 0 | PASS |
| 2. Ops/SRE reliability | Startup ordering, DDL defaults, Kafka publish failure | 0 | 0 | 0 | 0 | PASS |
| 3. Structural impact | Module boundary, compileOnly guards, Boot 4 modularization | 0 | 0 | 0 | 0 | PASS |
| 4. Kotlin 코드 품질 | 검증 helpers, public API KDoc, assertion style | 0 | 0 | 0 | 0 | PASS |
| 5. Tests/types/silent failure | Boot-created bean ordering, configured Kafka topic, schema defaults | 0 | 0 | 0 | 0 | PASS |
| 6. Performance/stability | Bounded Kafka send, opt-in DDL, no new background work | 0 | 0 | 0 | 0 | PASS |
| 7. Documentation/release/evidence | README locale parity, BOM docs, PR body evidence | 0 | 0 | 0 | 0 | PASS |

Final gate: `P0 = 0`, `P1 = 0`.

## 수정한 review finding

| Severity | 결과 | 수정 |
|---|---|---|
| P1 | `JaversSpringKafkaRepositoryAutoConfiguration`가 Spring Boot 4 Kafka auto-configuration이 `KafkaTemplate`을 만들기 전에 evaluate되어 `repository.type=spring-kafka`에서 조용히 back off할 수 있었다. | `afterName = ["org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"]`를 추가하고, `spring-boot-kafka` compile/test dependency를 추가했으며, Boot-created `KafkaTemplate`을 `ApplicationContextRunner`로 커버했다. |
| P2 | `bluetape4k.javers.kafka.topic`이 Spring Kafka repository path에서 무시됐다. | `KafkaCdoSnapshotRepository`와 `KafkaSnapshotEventPublisher`에 optional `topic`을 추가했고, auto-config가 validated topic을 전달하게 했다. |
| P2 | Exposed auto-config가 `initializeSchema=true`와 `createSchemaOnEnsure=true`를 통해 default로 DDL을 수행했다. | 두 default를 모두 `false`로 바꿔 explicit opt-in behavior를 보존했다. |
| P2 | BOM README locale pair가 새 module을 누락했다. | 두 BOM README 파일에 `bluetape4k-javers-spring-boot4-autoconfigure`를 추가했다. |
| P1 | `KafkaSnapshotEventPublisher.invoke(...)`에서 `keyMapper` 앞에 `topic`을 추가해 trailing-lambda caller의 source compatibility가 깨졌다. | 기존 companion `invoke(kafkaOperations, publishTimeout, keyMapper)` signature를 복원하고 explicit-topic publishing용 `withTopic(...)`을 추가했다. |
| P2 | public `KafkaCdoSnapshotRepository` primary constructor에 `topic`을 추가해 binary/source compatibility risk가 있었다. | old primary constructor를 복원하고 secondary topic constructor를 추가했다. `javap`로 old `(KafkaTemplate, Duration)` 및 default-mask constructor descriptor가 남아 있음을 확인했다. |
| P3 | production code가 `afterName` string만 사용하는데도 `spring-boot-kafka`가 production compile classpath에 있었다. | production `compileOnly("org.springframework.boot:spring-boot-kafka")`를 제거하고 Boot 4 class verification용 test dependency는 유지했다. |
| P3 | Type check가 boolean `is` assertion을 사용했다. | touched tests를 `shouldBeInstanceOf`로 전환했다. |
| P3 | Bean-presence assertion이 `shouldBeEqualTo true/false`를 사용했다. | touched tests를 `shouldBeTrue()` / `shouldBeFalse()`로 전환했다. |
| P3 | PR/review evidence wording이 CI/test completion 이후 stale했다. | tracked review evidence를 갱신했고 PR body는 current validation으로 refresh될 예정이다. |

## 증거

| 증거 | 결과 |
|---|---|
| Context7 Spring Boot 4 docs query for Kafka auto-configuration | `spring-boot-kafka`의 Spring Boot 4 Kafka auto-config class가 `org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration`임을 확인했다. |
| `gno query "bluetape4k-javers module registration CI Nightly coverage AutoConfiguration" -c bluetape4k-github --fast --no-rerank` | prior Spring Boot 4 example/module-registration PR evidence를 찾았다. |
| `gno query "bluetape4k-javers module registration CI Nightly coverage AutoConfiguration" -c bluetape4k-docs --no-rerank` | issue #140 Spring Boot 4 example research/review docs를 찾았다. |
| `rg "GlobalScope\|runBlocking\(\|Thread\.sleep\|delay\(\|synchronized\(\|@Synchronized\|runCatching\s*\{" javers-spring-boot4-autoconfigure/src/main/kotlin javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt` | Zero risky concurrency hits in touched production sources. |
| `rg "assertThat\|assertThrows\|kotlin\.test\|Assertions\." javers-spring-boot4-autoconfigure/src/test/kotlin javers-persistence-kafka/src/test/kotlin` | Zero forbidden assertion API hits in touched test sources. |
| `./gradlew :javers-persistence-kafka:compileKotlin :javers-persistence-kafka:compileTestKotlin :javers-spring-boot4-autoconfigure:compileKotlin :javers-spring-boot4-autoconfigure:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS. |
| `javap -classpath javers-persistence-kafka/build/classes/kotlin/main io.bluetape4k.javers.persistence.kafka.repository.KafkaCdoSnapshotRepository` | PASS; old public `(KafkaTemplate, Duration)` constructor and Kotlin default-mask constructor remain present, with a new topic constructor added. |
| `javap -classpath javers-persistence-kafka/build/classes/kotlin/main io.bluetape4k.javers.persistence.kafka.repository.KafkaSnapshotEventPublisher\\$Companion` | PASS; old `invoke(KafkaTemplate, Duration, Function1)` and `invoke$default` remain present, with `withTopic(...)` added. |
| `./gradlew :javers-spring-boot4-autoconfigure:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS; 17 tests executed, including Boot-created `KafkaTemplate` ordering and configured topic publishing. |
| `./gradlew :javers-persistence-kafka:cleanTest :javers-spring-boot4-autoconfigure:cleanTest :javers-persistence-kafka:test :javers-spring-boot4-autoconfigure:test :javers-spring-boot4-autoconfigure:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain` | PASS; Kafka executed 41 tests, auto-config executed 17 tests, and Kover XML was generated. |
| `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` | PASS. |
| `git diff --check` | PASS. |

## 비고

- `org.assertj:assertj-core`는 Spring Boot `ApplicationContextRunner`가 compile time에
  AssertJ-based API type을 노출하기 때문에만 존재한다. touched tests는 bluetape4k
  assertion을 사용한다.
- `spring-boot-kafka`는 Boot 4 auto-configuration metadata와 class-name ordering을
  위한 compile/test boundary로 사용된다. module은 여전히 application-owned Kafka
  infrastructure bean을 만들지 않는다.
