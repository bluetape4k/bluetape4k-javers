# Issue #136 - Vanilla Kafka Snapshot Publisher Plan

## Lane

Full Feature / Type A. 이 작업은 public repository class, dependency boundary
documentation, Kafka failure/lifecycle behavior, tests, README locale updates,
local reviews, PR을 추가한다.

## 작업

1. Step 0/1 - Setup and requirements
   - current repository evidence로 issue #136을 refresh한다.
   - current `origin/develop`에서 feature worktree를 만든다.
   - current Kafka repository, tests, Gradle dependencies, issue #40, sibling `bluetape4k-kafka` helper surface를 inspect한다.

2. Step 2/3 - Spec and plan review
   - design spec과 이 plan을 추가한다.
   - Step 2-R local 7-tier spec review를 실행한다.
   - Step 3-R local plan review를 실행한다.
   - 구현 전 P0=0 및 P1=0이 필요하다.

3. Step 4 - Implementation
   - `VanillaKafkaCdoSnapshotRepositoryOptions`를 추가한다.
   - `VanillaKafkaCdoSnapshotRepository`를 추가한다.
   - `JaversCodecs.String`과 `KafkaCdoSnapshotRepository`의 기존 write-only read contract pattern을 재사용한다.
   - topic과 timeout은 bluetape4k validation helper로 검증하고, matching helper가 없으면 standard `require`를 사용한다.
   - `bluetape4k-kafka`를 mandatory production runtime dependency로 추가하지 않는다.

4. Step 5 - Tests 및 docs
   - success capture, failure propagation, timeout propagation, interruption status, flush behavior, close ownership, validation, write-only read contract logging에 대한 vanilla repository tests를 추가한다.
   - `javers-persistence-kafka/README.md`를 갱신한다.
   - `javers-persistence-kafka/README.ko.md`를 갱신한다.

5. Step 6 - Verification
   - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`을 실행한다.
   - `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`을 실행하고 runtime classpath에 `spring-kafka`가 없음을 확인한다.
   - `git diff --check`를 실행한다.

6. Step 6-R - Final review
   - `docs/review/2026-06-08-issue-136-vanilla-kafka-publisher-review.md`를 추가한다.
   - P0=0 및 P1=0인 local 7-tier final review를 실행한다.

7. Step 7/8/9 - PR
   - `docs/lessons/2026-06-08-issue-136-vanilla-kafka-publisher.md`를 추가한다.
   - Lore trailer로 commit한다.
   - branch를 push한다.
   - assignee `debop`, milestone `0.3.0`으로 #136을 해결하는 PR을 만든다.
   - PR body file을 사용하고 live PR body를 검증하며 final `##` section이 `## DoD Status`인지 확인한다.
   - explicit user approval 없이 merge하지 않는다.

## 중단 조건

PR이 존재하고, PR body가 verified되고, validation이 recorded되며, local review가
P0=0/P1=0을 보고하면 중단한다. Merge는 별도의 user-approved action으로 남는다.

## 알려진 Risk

- `testRuntimeClasspath`에서 inspect하면 runtime dependency evidence가 misleading할 수 있으므로 production `runtimeClasspath`를 사용한다.
- Testcontainers-backed Kafka tests는 serial로 실행해야 한다.
- 기존 Kafka repository는 write-only다. tests는 explicit warning/default-return contract를 넘어선 read behavior를 assert하면 안 된다.
