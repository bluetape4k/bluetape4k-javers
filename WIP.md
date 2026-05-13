# WIP - bluetape4k-javers

Snapshot: 2026-05-13 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 3 issues.

## Recently Completed

- `bluetape4k-javers-bom` and localized BOM README files are merged by PR #10 and PR #11.
- JaVers implementation backlog was captured in repository docs by PR #12.
- Nightly smoke/full split, lessons guidance, dependency governance, compatibility guard, and Kover policy maintenance are merged through PR #14 through PR #24.

## Current Direction

The JaVers backlog is a strict phase chain. Do not start examples before the Exposed repository and DDD helper layer are usable.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P2 | [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3) javers-exposed | L | Phase 2. Implements Exposed JDBC `CdoSnapshotRepository`; prerequisite for the lane. |
| P2 | [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4) javers-ddd | M | Phase 3. Aggregate/domain-event helpers after `#3`. |
| P3 | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | M | Phase 4 example after `#4`. |

## Dependency Map

```text
#3 javers-exposed
  -> #4 javers-ddd
      -> #5 examples/javers-exposed-ddd
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| JaVers implementation | 1 | `#3` |
| Examples | 0 until implementation closes | `#5` waits. |
