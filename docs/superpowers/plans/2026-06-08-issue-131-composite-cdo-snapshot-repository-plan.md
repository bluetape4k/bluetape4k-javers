# Issue #131 - Composite CDO Snapshot Repository Plan

## Lane

Full Feature / Type A.

이 작업은 public repository composition API, failure-policy model, tests, README
locale updates, local 7-Tier reviews, milestone `0.3.0`용 PR을 추가한다.

## Step 0/1 Evidence

- Worktree: `.worktrees/feat-issue-131-composite-repository`.
- Base: `origin/develop@e54336d`.
- Live issue #131 body refreshed after #105 / PR #185 merge.
- 검토한 current contracts:
  - `CdoSnapshotRepository`
  - `AbstractCdoSnapshotRepository`
  - Kafka write-only repositories
  - `KafkaCdoSnapshotProjector`
  - #133 Redis + Exposed latency strategy
- GNO `bluetape4k-wiki` system-design query는 이 composite/fanout topic에 대한 direct match를 반환하지 않았다.

## 구현 작업

1. source edit 전에 Kotlin guidance를 재확인한다.
   - `bluetape4k-code-patterns`를 reload한다.
   - 계획된 새 core file과 기존 `CdoSnapshotRepository` surface에 대해 CodeGraph impact/context check를 실행한다.
   - `javers-core`에 기존 generic composite repository가 없는지 확인한다.

2. `javers-core`에 composite failure model을 추가한다.
   - `CompositeCdoSnapshotFailurePolicy`를 추가한다.
   - `CompositeCdoSnapshotDelegateKind`를 추가한다.
   - `CompositeCdoSnapshotWriteFailure`를 추가한다.
   - `CompositeCdoSnapshotException`을 추가한다.
   - English KDoc과 bluetape4k validation helper를 사용한다.
   - serializable data class는 `serialVersionUID`를 유지한다.

3. Composite option을 추가한다.
   - `CompositeCdoSnapshotRepositoryOptions`를 추가한다.
   - Defaults:
     - `writeFailurePolicy = FAIL_FAST`
     - `ensureSchemaFailurePolicy = FAIL_FAST`
     - `closeFailurePolicy = BEST_EFFORT`
   - validation 또는 invariant enforcement가 필요하면 private constructor와 companion `operator fun invoke(...)`를 사용한다.

4. `CompositeCdoSnapshotRepository`를 추가한다.
   - `CdoSnapshotRepository`를 직접 구현한다.
   - public read method와 `getHeadId()`는 primary에 delegate한다.
   - `setJsonConverter()`를 primary와 secondaries에 propagate한다.
   - `ensureSchema()`는 primary first로 propagate하고, 이후 options에 따라 secondaries에 propagate한다.
   - `saveSnapshot()`은 primary를 먼저 쓰고 ordered secondaries를 쓴다.
   - `persist(commit)`은 `primary.persist(commit)`을 먼저 호출한 뒤 ordered secondaries를 호출하므로 primary repository는 native head/sequence behavior를 유지한다.
   - Primary failure는 항상 secondary write를 막는다.
   - `FAIL_FAST`는 첫 secondary failure에서 중단한다.
   - `BEST_EFFORT`는 모든 secondary write를 시도한 뒤 aggregate failure를 throw한다.
   - `close()`는 모든 closeable delegate에 시도하고 aggregate failure를 보고한다.

5. core tests를 추가한다.
   - options defaults.
   - representative Javers read method와 `loadSnapshots`, `getHeadId`에 대한 read delegation.
   - `setJsonConverter()` propagation.
   - `ensureSchema()` propagation 및 failure policy.
   - primary-before-secondary write order.
   - primary failure가 secondary save를 막는지 검증.
   - `FAIL_FAST` secondary failure가 later secondaries를 중단하는지 검증.
   - `BEST_EFFORT` secondary failure가 모든 secondaries를 시도하고 aggregate하는지 검증.
   - `close()`가 모든 closeable을 시도하고 close failure를 aggregate하는지 검증.
   - Caffeine primary와 recording secondaries를 사용하는 JaVers commit.

6. docs를 갱신한다.
   - `javers-core/README.md`를 갱신한다.
   - `javers-core/README.ko.md`를 갱신한다.
   - recommended Exposed + Kafka 및 Exposed + Redis + Kafka shape를 포함한다.
   - non-atomicity, failure policies, Kafka write-only boundary를 명시한다.
   - root module overview에 짧은 cross-reference가 필요할 때만 root README locale pair를 갱신한다.

7. review 및 lesson artifact를 추가한다.
   - `docs/review/2026-06-08-issue-131-composite-cdo-snapshot-repository-review.md`를 추가한다.
   - `docs/lessons/2026-06-08-issue-131-composite-cdo-snapshot-repository.md`를 추가한다.

## 검증 작업

1. touched Kotlin에 대한 static/pattern scan:
   - no `!!`
   - no `runBlocking` in production
   - no `GlobalScope`
   - no `synchronized` / `@Synchronized`
   - no raw JUnit assertion APIs in new tests
   - data classes are `Serializable` and define `serialVersionUID`
2. Targeted tests:
   - `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`
3. Kafka contract guard:
   - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
4. Diff checks:
   - `git diff --check`
5. Step 6-R local 7-Tier code review:
   - P0 = 0
   - P1 = 0
6. PR gate:
   - Lore trailer로 commit한다.
   - branch를 push한다.
   - assignee `debop`, milestone `0.3.0`으로 #131을 해결하는 PR을 만든다.
   - `--body-file`을 사용하고 live PR body를 검증하며 final section이 `## DoD Status`인지 확인한다.
   - explicit user approval 없이 merge하지 않는다.

## 기각한 대안

- composite를 `javers-persistence-kafka`에 둔다: Exposed, Redis, Caffeine, Kafka가 module dependency cycle 없이 shared `CdoSnapshotRepository` contract를 통해 compose되어야 하므로 기각한다.
- `AbstractCdoSnapshotRepository`를 확장한다: composite는 codec serialization, commit sequence storage, head restoration을 소유하지 않으므로 기각한다.
- 새 JaVers cache abstraction을 추가한다: #131 및 #133 reuse constraint 때문에 기각한다.
- automatic retry/outbox/compensation을 추가한다: 이 issue를 넘어 reliability 및 operational semantics를 바꾸므로 기각한다.
- secondary failure 시 primary storage를 roll back한다: 기존 `CdoSnapshotRepository` implementation은 safe rollback API를 노출하지 않고 #131이 distributed transaction semantics를 명시적으로 피하므로 기각한다.
- Kafka repository를 read-capable하게 만든다: #105가 이미 Kafka write-only repository를 보존하면서 explicit projector를 추가했으므로 기각한다.

## 중단 조건

PR이 존재하고, live PR body가 verified되고, local validation이 통과하고, Step 6-R이
P0=0/P1=0을 보고하며, CI status가 user-approved merge 준비 상태이면 중단한다.

Merge는 별도의 user-approved action으로 남는다.
