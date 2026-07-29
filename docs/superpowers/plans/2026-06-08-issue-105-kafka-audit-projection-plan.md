# Issue #105 - Kafka Audit Projection 계획

## 실행 Lane

Full Feature / Type A. 이 작업은 public projection API, replay semantics,
Kafka/Redis integration coverage, README locale updates, local reviews, PR을
추가한다.

## 작업

1. Step 0/1 - Setup and requirements
   - current repository evidence로 issue #105를 refresh한다.
   - current `origin/develop`에서 feature worktree를 만든다.
   - 현재 Kafka publishers, #89 example projection, Redis repository target, bluetape4k-kafka helper를 inspect한다.

2. Step 2/3 - Spec and plan review
   - design spec과 이 plan을 추가한다.
   - plan이 write-only Kafka repository behavior를 보존하는지 확인한다.
   - target read store가 기존 `CdoSnapshotRepository`인지 확인한다.
   - 구현 전 P0=0 및 P1=0이 필요하다.

3. Step 4 - 구현
   - `KafkaCdoSnapshotProjectionOptions`를 추가한다.
   - `KafkaCdoSnapshotProjectionResult`를 추가한다.
   - `KafkaCdoSnapshotProjector`를 추가한다.
   - repository-owned consumer convenience constructor에는 `bluetape4k-kafka` `consumerOf(...)`를 재사용한다.
   - current wire value에는 기존 `JaversCodecs.String`과 JaVers `JsonConverter`를 재사용한다.
   - Kafka repository read method는 변경하지 않는다.

4. Step 5 - Tests 및 docs
   - validation, duplicate skip, failure offset behavior, consumer ownership에 대한 unit tests를 추가한다.
   - `LettuceCdoSnapshotRepository`로 project하는 Kafka Testcontainers integration을 추가한다.
   - `javers-persistence-kafka/README.md`를 갱신한다.
   - `javers-persistence-kafka/README.ko.md`를 갱신한다.

5. Step 6 - Verification
   - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`을 실행한다.
   - `git diff --check`를 실행한다.

6. Step 6-R - 최종 검토
   - `docs/review/2026-06-08-issue-105-kafka-audit-projection-review.md`를 추가한다.
   - P0=0 및 P1=0인 local 7-tier final review를 실행한다.

7. Step 7/8/9 - PR
   - implementation이 durable workflow guidance를 드러내면 짧은 lesson을 추가한다.
   - Lore trailer로 commit한다.
   - branch를 push한다.
   - assignee `debop`, milestone `0.3.0`으로 #105를 해결하는 PR을 만든다.
   - PR body file을 사용하고 live PR body를 검증하며 final `##` section이 `## DoD Status`인지 확인한다.
   - explicit user approval 없이 merge하지 않는다.

## 중단 조건

PR이 존재하고, PR body가 verified되고, validation이 recorded되며, local review가
P0=0/P1=0을 보고하면 중단한다. Merge는 별도의 user-approved action으로 남는다.

## 알려진 위험

- Kafka와 Redis Testcontainers는 serial로 실행해야 한다.
- 현재 Kafka wire value는 여전히 encoded snapshot payload뿐이다.
- Replay는 target repository에 이미 존재하는 exact duplicate snapshot에 대해서는 idempotent지만, exactly-once Kafka transaction guarantee는 아니다.
