# Issue #136 - Vanilla Kafka Snapshot Publisher Review

## 범위

Issue #136에서 계획하고 구현한 scope:

- `docs/superpowers/specs/2026-06-08-issue-136-vanilla-kafka-publisher-design.md`
- `docs/superpowers/plans/2026-06-08-issue-136-vanilla-kafka-publisher-plan.md`
- `javers-persistence-kafka` production source, tests, and README locale pair
- `javers-persistence-kafka` Kafka test fixture IDs

이 review file은 branch가 Step 2-R, Step 3-R, Step 6-R을 지나는 동안 갱신된다.

## Step 2-R Spec Review

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Kafka producer API, encoded payload, dependency boundary | 새 deserialization 또는 input trust boundary를 도입하지 않는다. Repository는 encoded JaVers JSON만 publish한다. 해당 artifact가 Spring support를 포함하므로 spec은 mandatory `bluetape4k-kafka` runtime dependency를 피한다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Timeout, failure propagation, lifecycle ownership | Spec은 positive `publishTimeout`, publish failure propagation, interrupt preservation, optional flush, explicit close ownership을 요구한다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module/API boundary | Public API는 기존 Kafka module 안의 vanilla repository와 options object로 제한된다. 새 module 또는 read projection behavior는 도입하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API | Constructor shape, validation, KDoc | Options object는 same-typed boolean parameter ambiguity를 피한다. KDoc과 README는 상황에 맞게 English/Korean으로 필요하다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Testability | Failure, timeout, interruption, lifecycle, warning contract | Spec은 모든 runtime behavior와 dependency boundary에 대한 concrete test를 명시한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/Stability | Blocking send wait, flush behavior | Spec은 blocking `Future.get(timeout)`을 bounded하게 유지하고 `flushAfterSend`를 opt-in으로 만든다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/Release | README locale pair, issue links, evidence | Spec은 README locale parity를 요구하고 #105/#131을 non-goal로 기록한다. CI/Nightly/module registration 변경은 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |

Step 2-R 판정: P0=0, P1=0으로 PASS.

## Step 3-R Plan Review

| 관점 | 결과 | 필요한 수정 | Counts |
|---|---|---|---|
| Implementer | task는 evidence, spec/plan review, implementation, tests/docs, validation, final review, PR 순서로 정렬되어 있다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Test engineer | plan은 각 spec behavior를 named test에 매핑하고 serial Kafka module verification을 사용한다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Architect | plan은 Spring adapter를 보존하고 vanilla adapter를 추가하며, read projection/composite behavior를 scope 밖에 둔다. | 없음. | P0=0, P1=0, P2=0, P3=0 |
| Delivery | plan은 README locale pair, lesson, review artifact, dependency evidence, PR body verification, milestone, assignee를 커버한다. | 없음. | P0=0, P1=0, P2=0, P3=0 |

Step 3-R 판정: P0=0, P1=0으로 PASS.

## Step 6-R Final Review

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | `VanillaKafkaCdoSnapshotRepository`, options, README | credential 또는 auth surface를 추가하지 않았다. Encoded JaVers JSON만 publish한다. deserialization 또는 class-loading boundary는 도입하지 않는다. Runtime dependency evidence는 Spring Kafka를 vanilla production path 밖에 유지한다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Publish error path, timeout, interruption, lifecycle | Publish는 positive `publishTimeout`으로 bounded된다. failure는 propagate된다. `InterruptedException`은 interrupt status를 복원한다. producer close는 explicit 및 opt-in이다. 테스트는 각 behavior를 커버한다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Module boundary와 API compatibility | 기존 `KafkaCdoSnapshotRepository`는 변경하지 않았다. 새 class는 Apache `Producer`를 직접 받으며 mandatory `bluetape4k-kafka` 또는 Spring runtime dependency를 추가하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin quality | Production Kotlin과 tests | Public API KDoc은 English다. options data class는 `Serializable`이다. topic validation은 bluetape4k `requireNotBlank`를 사용한다. matching helper가 없어서 timeout은 standard `require`를 사용한다. constructor access는 이제 companion `invoke`를 따른다. MockK producer/metadata mock은 `@BeforeEach clearMocks`로 reset되는 class field다. Kafka test fixture unique ID는 `Base58.randomString`을 사용한다. production scan에서 `!!`, `runBlocking`, `GlobalScope`, synchronization, `runCatching` hit는 없다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | `VanillaKafkaCdoSnapshotRepositoryTest`, existing Kafka tests | 테스트는 success payload, custom key mapping, failure propagation, timeout, interruption, flush, close ownership, validation, head rebuild behavior, write-only warning parity를 커버한다. Targeted module tests는 18 tests를 실행했다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Blocking send wait, flush, close, tests | Blocking wait는 caller-configured timeout으로 bounded된다. `flushAfterSend`는 기본값 false이며 opt-in으로 테스트된다. unbounded polling, retry, buffer, coroutine cancellation surface는 도입하지 않았다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | README locale pair, spec/plan/review evidence | README English/Korean pair는 adapter selection과 optional `bluetape4k-kafka` helper boundary로 갱신했다. module registration, CI, Nightly, BOM, changelog update는 필요하지 않다. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R 판정: P0=0, P1=0으로 PASS.

## Post-PR Review Comment 후속 조치

- Thread `PRRT_kwDOSVj8-s6Hqnle`: `VanillaKafkaCdoSnapshotRepository` constructor를 private으로 만들고 companion `operator fun invoke(...)` factory를 노출해 처리했다.
- Thread `PRRT_kwDOSVj8-s6Hqn90`: producer 및 metadata MockK instance를 class field로 옮기고 `@BeforeEach`에서 clear하도록 처리했다.
- Additional `bluetape4k-code-patterns` sweep: Kafka test fixture `UUID.randomUUID().encodeUrl62()` ID를 `Base58.randomString(8)` suffix로 교체했다.
- Follow-up verification: `:javers-persistence-kafka:test` PASS, 18 tests executed; `git diff --check` PASS.

## 검증 증거

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 18 tests executed.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "spring-kafka|bluetape4k-kafka|kafka-clients"`
  - 결과: `org.apache.kafka:kafka-clients:4.2.0`만 나타났다. production runtime classpath에는 `spring-kafka` 또는 `bluetape4k-kafka`가 나타나지 않았다.
- `git diff --check`
  - 결과: PASS, no whitespace errors.

## Final Gate 판정

P0=0. P1=0. PR creation is allowed.
