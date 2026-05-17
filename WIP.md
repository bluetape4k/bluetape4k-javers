# WIP - bluetape4k-javers

Snapshot: 2026-05-17 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 7 issues.

## Recently Completed

- `bluetape4k-javers-bom` and localized BOM README files are merged by PR #10 and PR #11.
- JaVers implementation backlog was captured in repository docs by PR #12.
- Nightly smoke/full split, lessons guidance, dependency governance, compatibility guard, and Kover policy maintenance are merged through PR #14 through PR #24.
- **14 pre-release bug/typo fixes** merged by PR #47, PR #48, and PR #49:
  - PR #47: API typo renames (DebugDispatcher, EntityEnvelope, CompressibleString/BinaryJaversCodec) + P0 correctness fixes (saveSnapshot exception propagation, encode NPE, ShadowProvider !! removal).
  - PR #48: Runtime safety — @Volatile head, locked loadSnapshots, dedicated Lettuce MULTI/EXEC connection, Kafka 30 s publish timeout, write-only WARN logging, getAll() OOM guard.
  - PR #49: Test fixes — no-op assertion corrected, JCacheCommitTest added, Kafka write-only @Disabled overrides and publish-failure coverage added.

## Current Direction

Pre-release prep (0.1.0) and JaVers feature phase chain.

Do not start examples before the Exposed repository and DDD helper layer are usable (#3 → #4 → #5).

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P0 | [#29](https://github.com/bluetape4k/bluetape4k-javers/issues/29) Update bluetape4k dependency | S | Bump SNAPSHOT → 1.8.0 release before 0.1.0 publish. |
| P0 | [#30](https://github.com/bluetape4k/bluetape4k-javers/issues/30) Translate KDoc to English | M | Required for public API KDoc policy. |
| P0 | [#31](https://github.com/bluetape4k/bluetape4k-javers/issues/31) Stamp CHANGELOG 0.1.0 | S | Replace [Unreleased] with [0.1.0] + date before release. |
| P0 | [#32](https://github.com/bluetape4k/bluetape4k-javers/issues/32) Verify Maven Central secrets | S | Confirm GitHub Actions publish secrets before release. |
| P2 | [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3) javers-exposed | L | Phase 2. Implements Exposed JDBC `CdoSnapshotRepository`; prerequisite for the lane. |
| P2 | [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4) javers-ddd | M | Phase 3. Aggregate/domain-event helpers after `#3`. |
| P3 | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | M | Phase 4 example after `#4`. |

## Dependency Map

```text
#29 → #30 → #31 → #32  (release-prep chain)

#3 javers-exposed
  -> #4 javers-ddd
      -> #5 examples/javers-exposed-ddd
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Release prep | 1 | `#29` → `#30` → `#31` → `#32` |
| JaVers implementation | 1 | `#3` (after release prep) |
| Examples | 0 until implementation closes | `#5` waits. |
