# WIP - bluetape4k-javers

Snapshot: 2026-05-18 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 4 issues.

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

## Current Direction

Redis persistence correctness and JaVers feature phase chain.

Fix persistent repository head restoration before expanding Redis-backed examples.
Do not start examples before the Exposed repository and DDD helper layer are usable (#3 → #4 → #5).

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#62](https://github.com/bluetape4k/bluetape4k-javers/issues/62) Persistent JaVers repositories lose head commit across rebuilds | M | Lettuce/Redisson store snapshots and commit sequences but `getHeadId()` is only in-memory. Add restart regression coverage. |
| P2 | [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3) javers-exposed | L | Phase 2. Implements Exposed JDBC `CdoSnapshotRepository`; prerequisite for the lane. |
| P2 | [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4) javers-ddd | M | Phase 3. Aggregate/domain-event helpers after `#3`. |
| P3 | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | M | Phase 4 example after `#4`. |

## Dependency Map

```text
#62 persistent repository head restoration
  -> Redis-backed production safety before more examples

#3 javers-exposed
  -> #4 javers-ddd
      -> #5 examples/javers-exposed-ddd
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness | 1 | `#62` |
| JaVers implementation | 1 | `#3` (after correctness check) |
| Examples | 0 until implementation closes | `#5` waits. |
