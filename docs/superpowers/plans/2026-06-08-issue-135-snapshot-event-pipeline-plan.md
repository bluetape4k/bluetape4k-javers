# Issue #135 - Snapshot Event Pipeline Plan

## 목표

transport-neutral JaVers snapshot event publishing contract를 도입하고, vanilla Kafka
producer creation에는 governed `bluetape4k-kafka` helper dependency를 사용하면서 기존
Kafka repository path를 여기에 맞춘다.

## Step 0/1 Evidence

- Worktree: `.worktrees/feat-issue-135-snapshot-event-pipeline`.
- Base: `origin/develop@9c63a0d`.
- Issue #135 body refreshed on 2026-06-08 KST.
- Current implementation: Spring Kafka 및 vanilla Kafka repository는 write-only이고 encoded string snapshot을 publish한다.
- Research: `docs/research/2026-06-04-javers-multilayer-cache-pipeline.md`.
- User scope update: raw Kafka client helper construction을 module dependency graph 밖에 두는 대신 `bluetape4k-kafka`를 사용한다.

## 구현 작업

1. core event contract를 추가한다.
   - `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/event/`를 만든다.
   - `CdoSnapshotEventMetadata`, `CdoSnapshotEvent<T>`, `CdoSnapshotEventPublisher<T>`, codec id constants를 추가한다.
   - English KDoc과 example을 포함한다.
   - non-blank value에는 bluetape4k validation helper를 사용한다.
   - serializable value object가 `serialVersionUID`를 정의하게 한다.

2. core tests를 추가한다.
   - committed `CdoSnapshot`에서 metadata extraction을 test한다.
   - idempotency key stability를 test한다.
   - nullable repository sequence behavior를 test한다.
   - bluetape4k assertions만 사용한다.

3. Kafka publisher adapter를 추가한다.
   - `KafkaTemplate<String, String>` backed `KafkaSnapshotEventPublisher`를 추가한다.
   - `Producer<String, String>` backed `VanillaKafkaSnapshotEventPublisher`를 추가한다.
   - timeout, failure propagation, interrupt status, key mapping, vanilla flush, close ownership behavior를 보존한다.
   - direct Spring Kafka declaration은 `compileOnly`로 유지한다. dependency evidence에는 여전히 `bluetape4k-kafka`를 통해 Spring Kafka가 transitively 보일 수 있다.
   - vanilla producer factory overload에는 `bluetape4k-kafka` `producerOf(...)`를 사용한다.

4. Kafka repository를 refactor한다.
   - `KafkaCdoSnapshotRepository.saveSnapshot()` builds a
     `CdoSnapshotEvent<String>` and delegates to `KafkaSnapshotEventPublisher`.
   - `VanillaKafkaCdoSnapshotRepository.saveSnapshot()` builds a
     `CdoSnapshotEvent<String>` and delegates to
     `VanillaKafkaSnapshotEventPublisher`.
   - write-only read path warning과 기존 public constructor / companion factory ergonomics를 보존한다.

5. Kafka tests를 추가/갱신한다.
   - `@BeforeEach clearMocks`로 reset되는 class-level MockK field를 사용하는 adapter-specific unit tests를 추가한다.
   - 기존 repository behavior tests를 계속 통과시킨다.
   - repository-created record가 여전히 global id key와 encoded payload를 사용함을 증명하는 tests를 추가한다.

6. docs를 갱신한다.
   - `javers-persistence-kafka/README.md`를 갱신한다.
   - `javers-persistence-kafka/README.ko.md`를 갱신한다.
   - transport selection table과 NATS/SQS design artifact를 추가한다.
   - #105 및 #131 boundary를 언급한다.

7. review 및 lesson artifact를 갱신한다.
   - `docs/review/2026-06-08-issue-135-snapshot-event-pipeline-review.md`를 추가한다.
   - `docs/lessons/2026-06-08-issue-135-snapshot-event-pipeline.md`를 추가한다.

## 검증 작업

1. 사용 가능하면 Kotlin edit 전에 CodeGraph 또는 동등한 impact check를 수행한다.
2. Production pattern scan:
   - no `GlobalScope`, `runBlocking`, `Thread.sleep`, `synchronized`,
     `@Synchronized`, `runCatching`, `!!`, or UUID-derived suffixes in touched
     production/test files.
3. Targeted tests:
   - `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
4. Runtime dependency evidence:
   - `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg -n "bluetape4k-kafka|spring-kafka|bluetape4k-nats|aws|sqs|kafka-clients"`
5. `git diff --check`.
6. Step 6-R 7-tier review with P0=0 and P1=0.
7. PR body는 `--body-file`로 만들고 live에서 검증한다. final section은 `## DoD Status`여야 한다.

## 기각한 대안

- 지금 NATS implementation을 추가한다: #135는 testable design artifact로 non-Kafka adapter requirement를 닫을 수 있고, runtime dependency 추가는 CI와 dependency governance를 넓히므로 기각한다.
- 지금 SQS implementation을 추가한다: repository catalog에 이 module용 SQS/AWS SDK alias가 없으므로 기각한다.
- Kafka repository를 read-capable하게 만든다: #105가 read projection을 소유하고 #131이 composite durable history plus event stream을 소유하므로 기각한다.
- asynchronous background buffering을 추가한다: current repository head semantics는 `persist()`가 commit head를 mark하기 전 publish failure propagation에 의존하므로 기각한다.

## 기대 DoD

- Core event contract가 존재하고 test된다.
- 기존 Kafka repository behavior는 publisher adapter를 통해 보존된다.
- NATS JetStream 및 SQS adapter semantics는 design artifact로 문서화된다.
- Kafka helper에는 `bluetape4k-kafka`를 사용한다. 새 NATS/SQS/AWS runtime dependency는 도입하지 않는다.
- local tests, dependency check, diff check, Step 6-R review가 통과한다.
