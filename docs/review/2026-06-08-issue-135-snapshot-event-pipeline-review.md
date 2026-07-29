# Issue #135 - Snapshot Event Pipeline 검토

## 범위

- `docs/superpowers/specs/2026-06-08-issue-135-snapshot-event-pipeline-design.md`
- `docs/superpowers/plans/2026-06-08-issue-135-snapshot-event-pipeline-plan.md`
- 계획된 `javers-core` event contract
- 계획된 `javers-persistence-kafka` publisher adapter 및 README locale pair

## Step 2-R Spec 검토

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Event metadata와 payload | 새 deserialization, credential, auth boundary를 도입하지 않는다. Metadata는 trusted JaVers snapshot object에서 파생된다. SQS/NATS는 design artifact로 남겨 unreviewed client configuration을 피한다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Publish acknowledgement와 failure | Spec은 synchronous acknowledgement와 failure propagation을 보존하므로 publish failure 시 repository head가 advance되지 않는다. Interrupt preservation이 필요하다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Core API와 Kafka module boundary | Core contract는 transport-neutral이며 dependency-free다. Kafka adapter는 Kafka module에 남고 governed `bluetape4k-kafka` helper dependency를 사용한다. #105와 #131은 scope 밖에 남는다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Public API shape | Serializable value object, fun-interface publisher, English KDoc, explicit nullable repository sequence가 API를 idiomatic하고 오용하기 어렵게 유지한다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Testability | Metadata와 adapter behavior | Spec은 metadata, idempotency, adapter delegation, timeout/failure, interrupt, flush, close ownership에 대한 concrete unit test를 명시한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/Stability | Blocking과 buffering | background queue, retry loop, batching을 도입하지 않는다. Kafka publish wait는 configured timeout으로 bounded된다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Docs/Release | README와 dependency governance | README locale pair와 design artifact가 필요하다. `bluetape4k-kafka`는 이미 catalog로 governed되므로 module registration, CI/Nightly, BOM 변경은 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |

Step 2-R 판정: P0=0, P1=0으로 PASS.

## Step 3-R Plan 검토

| 관점 | 결과 | 필요한 수정 | Counts |
|---|---|---|---|
| Implementer | task는 core contract에서 adapter, repository refactor, tests, docs, review 순서로 정렬되어 있다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Test engineer | plan은 모든 behavior를 targeted core/Kafka tests에 매핑하고 serial Gradle command를 명시한다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Architect | plan은 dependency-free contract를 core에 두고 transport-specific code를 Kafka module에 둔다. `bluetape4k-kafka`는 Kafka module로 제한한다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Delivery | plan은 README English/Korean, review artifact, lesson, dependency evidence, PR body rule, CI gate를 커버한다. | 없음. | P0=0, P1=0, P2=0, P3=0 |

Step 3-R 판정: P0=0, P1=0으로 PASS.

## Step 6-R 최종 검토

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Event payload와 metadata | 새 deserialization path, credential handling, auth boundary를 추가하지 않았다. Publisher는 이미 encoded된 JaVers JSON string을 forward하고 transport failure를 propagate한다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness | Repository head와 failure semantics | 두 Kafka repository는 `CdoSnapshotEvent<String>`을 build하고 synchronous publisher에 delegate한다. Publish failure는 head advancement 전에 `persist()`를 계속 abort한다. Interrupt status는 복원된다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Dependency와 module boundary | Core event API는 `javers-core`에 머물고 Kafka dependency가 없다. Kafka adapter는 `javers-persistence-kafka`에 머문다. vanilla producer factory는 governed `bluetape4k-kafka`를 사용한다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Data class validation과 KDoc | Constructor validation은 private constructor 및 `@ConsistentCopyVisibility`와 함께 companion `invoke` pattern을 따른다. Public API는 English KDoc을 가진다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests | Core와 Kafka behavior | Core metadata tests는 snapshot extraction, validation, event payload를 커버한다. 기존 Spring/vanilla Kafka repository tests는 publisher adapter를 통과한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Docs | README locale pair와 design artifacts | English/Korean README files는 event contract, Kafka adapter selection, planned NATS/SQS boundary를 설명한다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Delivery | Issue 및 workflow evidence | plan, spec, issue body는 `bluetape4k-kafka` scope에 맞게 갱신했다. 새 module, CI, Nightly, BOM registration은 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |

Final 판정: P0=0, P1=0으로 PASS.

## 엄격 재검토 후속 조치

Initial review는 implementation 중심으로 치우쳐 있었다. 더 엄격한 pass에서 다음을 발견하고 수정했다.

| Severity | Location | 결과 | 수정 |
|---|---|---|---|
| P2 | `javers-persistence-kafka/build.gradle.kts` | `bluetape4k-kafka`를 internal helper로 사용했지만 `api`로 선언해 compile classpath가 consumer에 leak될 수 있었다. | `implementation(libs.bluetape4k.kafka)`로 변경했고 `api` dependencies에 Kafka helper dependency가 포함되지 않음을 검증했다. |
| P2 | `KafkaCdoSnapshotRepository.saveSnapshot`, `VanillaKafkaCdoSnapshotRepository.saveSnapshot` | Trace log가 audit data와 user field를 포함할 수 있는 full encoded snapshot payload를 포함했다. | Trace log를 key, snapshot version, codec metadata로만 줄였다. |
| P2 | Kafka tests | 새 public helper path와 Spring interrupt behavior의 test가 부족했다. | `bluetape4k-kafka` `producerOf(...)`를 통한 repository-created producer test와 Spring Kafka interrupt-preservation test를 추가했다. |
| P3 | PR review comments | Explicit publisher key parameter가 blank value를 guard하지 않았다. | Spring 및 vanilla publisher explicit-key path에 `key.requireNotBlank("key")`를 추가하고 blank-key regression tests를 추가했다. |

수정 후 strict re-review 판정: P0=0, P1=0으로 PASS.

## 검증 증거

- `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: BUILD SUCCESSFUL; `javers-core` executed 184 tests; `javers-persistence-kafka` executed 20 tests.
- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PR comment fix 이후 BUILD SUCCESSFUL; `javers-persistence-kafka` executed 22 tests.
- `./gradlew :javers-persistence-kafka:dependencies --configuration api --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|kafka-clients|No dependencies|io.github.bluetape4k|org.apache.kafka"`
  - 결과: `api` dependencies는 의도한 bluetape4k API module만 포함한다. `bluetape4k-kafka`, `spring-kafka`, `kafka-clients` API leak은 나타나지 않았다.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|bluetape4k-nats|aws|sqs|kafka-clients"`
  - 결과: `bluetape4k-kafka`와 `kafka-clients`는 존재한다. `bluetape4k-nats`, AWS, SQS runtime dependency는 나타나지 않았다. `spring-kafka`는 `bluetape4k-kafka`를 통해 transitively 나타난다. direct module declaration은 `compileOnly`로 남는다.
- `rg -n "GlobalScope|runBlocking|Thread\\.sleep|synchronized\\s*\\(|@Synchronized|runCatching|!!|UUID" ...`
  - 결과: touched source/test path에서 match 없음.
- `git diff --check`
  - 결과: PASS.
