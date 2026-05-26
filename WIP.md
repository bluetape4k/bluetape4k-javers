# WIP - bluetape4k-javers

Snapshot: 2026-05-26 KST
Scope: 0.2.0 feature phase chain after #3/#77 merge and #4 implementation.
Open count: tracked from GitHub at work start; #4 is implemented in the current branch and waits for PR review/merge.

## 2026-05-26 Issue #4 Update

Current evidence:

- #77 README persistence diagram merged by PR #85.
- #3 `javers-exposed` merged by PR #86.
- #4 `javers-ddd` implementation is complete in this branch:
  - new `javers-ddd` module,
  - `AggregateRoot`, `DomainEvent`, `AggregateRepository`,
  - Spring ApplicationEvent, Kafka, and NATS publisher adapters,
  - H2 + `ExposedCdoSnapshotRepository` aggregate repository tests,
  - CI/Nightly wiring.

Next implementation item after #4 merge: #5 CQRS/Event Sourcing example.

## 2026-05-24 Milestone Refresh

Historical evidence: latest tags `0.1.2`, `0.1.1`, `0.1.0`. GitHub had one
unmilestoned bug (#62) and three `0.2.0` feature issues (#3, #4, #5).

| Lane | Candidate milestone | Current candidates | Decision |
|---|---|---|---|
| Patch | `0.1.3` | #62 | Persistent repository head restoration is a patch/correctness issue. Assign it before continuing feature phases. |
| Minor | `0.2.0` | #3 -> #4 -> #5 | Keep the existing phase chain: Exposed snapshot repository, DDD helper layer, then CQRS/Event Sourcing example. |

Current order: #62 remains the correctness lane; #3 and #77 are done; #4 is
implemented; #5 waits for #4 merge.

## New Milestone Queue - 2026-05-24

### New patch milestone `0.1.3`

1. [#62](https://github.com/bluetape4k/bluetape4k-javers/issues/62)
   `bug: persistent JaVers repositories lose head commit across rebuilds`
2. [#76](https://github.com/bluetape4k/bluetape4k-javers/issues/76)
   `test: add restart and rebuild contract tests for Redis and Kafka repositories`

### New minor milestone `0.2.0`

1. [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3)
   `[feat] javers-exposed — ExposedCdoSnapshotRepository JDBC 구현 (Phase 2)`
2. [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4)
   `[feat] javers-ddd — AggregateRoot / DomainEvent DDD 패턴 헬퍼 (Phase 3)`
3. [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5)
   `[feat] examples/javers-exposed-ddd — CQRS / Event Sourcing 데모 (Phase 4)`
4. [#77](https://github.com/bluetape4k/bluetape4k-javers/issues/77)
   `docs: add README relationship diagram for Redis Kafka and Exposed persistence options`

## Issue Discovery - 2026-05-24

Patch candidates:

- `bug: persistent JaVers repositories lose head commit across rebuilds` (#62)
- `test: add restart/rebuild contract tests for Redis and Kafka repositories`
  - Candidate follow-up if #62 proves the gap is broader than one repository
    implementation.

Minor candidates:

- `feat: javers-exposed ExposedCdoSnapshotRepository JDBC implementation` (#3)
- `feat: javers-ddd AggregateRoot and DomainEvent helper layer` (#4)
- `docs/examples: javers-exposed-ddd CQRS and Event Sourcing demo` (#5)
- `docs: add README relationship diagram for Redis/Kafka/Exposed persistence options`
  - Candidate after #3 lands so the diagram can reflect actual implementation.

## Recently Completed

- `bluetape4k-javers-bom` and localized BOM README files are merged by PR #10 and PR #11.
- JaVers implementation backlog was captured in repository docs by PR #12.
- Nightly smoke/full split, lessons guidance, dependency governance, compatibility guard, and Kover policy maintenance are merged through PR #14 through PR #24.
- 0.1.0 pre-release prep is closed:
  - #29 bumped the bluetape4k dependency.
  - #30 translated public KDoc to English.
  - #31 stamped the CHANGELOG for 0.1.0.
  - #32 verified Maven Central release secrets.
- **14 pre-release bug/typo fixes** merged by PR #47, PR #48, and PR #49:
  - PR #47: API typo renames (DebugDispatcher, EntityEnvelope, CompressibleString/BinaryJaversCodec) + P0 correctness fixes (saveSnapshot exception propagation, encode NPE, ShadowProvider !! removal).
  - PR #48: Runtime safety — @Volatile head, locked loadSnapshots, dedicated Lettuce MULTI/EXEC connection, Kafka 30 s publish timeout, write-only WARN logging, getAll() OOM guard.
  - PR #49: Test fixes — no-op assertion corrected, JCacheCommitTest added, Kafka write-only @Disabled overrides and publish-failure coverage added.
- QMD matched the 0.1.0 pre-release fix lesson; GitHub issue state is the source of truth for this refresh.
- Milestone `0.1.1` has zero open issues after #60 and #63; it is ready for release.
- PR #85 completed #77 README persistence options diagram.
- PR #86 completed #3 `javers-exposed`.
- Current branch implements #4 `javers-ddd`; keep #5 blocked until #4 is merged.

## Current Direction

Redis persistence correctness and JaVers feature phase chain.

The 0.1.1 release gate is clear. Keep #62 as the next correctness item unless it is explicitly assigned to a release milestone.

Do not start examples before the DDD helper layer is merged (#4 → #5).

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#62](https://github.com/bluetape4k/bluetape4k-javers/issues/62) Persistent JaVers repositories lose head commit across rebuilds | M | Lettuce/Redisson store snapshots and commit sequences but `getHeadId()` is only in-memory. Add restart regression coverage. |
| Done | [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3) javers-exposed | L | Phase 2 merged by PR #86. |
| In review | [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4) javers-ddd | M | Phase 3 implemented in current branch; merge before #5. |
| P3 | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | M | Phase 4 example after #4 merge. |

## Dependency Map

```text
#62 persistent repository head restoration
  -> Redis-backed production safety before more examples

#3 javers-exposed (done)
  -> #4 javers-ddd (current branch)
      -> #5 examples/javers-exposed-ddd (next after merge)
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness | 1 | `#62` |
| JaVers implementation | 1 | `#4` merge/review |
| Examples | 0 until #4 closes | `#5` waits. |
