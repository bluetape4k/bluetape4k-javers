# WIP - bluetape4k-javers

Snapshot: 2026-06-26 KST
Scope: open GitHub issues assigned to `debop`.
Open count: 12 issues.

## Current Direction

The `0.2.1` patch lane has been published and consumed by
`bluetape4k-dependencies` `1.2.0`. Development is now on the `0.3.0` line with
`snapshotVersion=` kept empty for workflow-injected snapshot publication.

Release-prep work should finish the eleven remaining `0.3.0` issues before the
next `bluetape4k-dependencies` release train consumes a stable
`bluetape4k-javers-bom`.

## Milestone Snapshot

| Milestone | Open | Closed | Notes |
|---|---:|---:|---|
| `0.3.0` | 11 | 63 | Release-blocking queue for the next stable javers BOM. |
| `backlog` | 1 | 4 | Future R2DBC persistence research; not a `0.3.0` blocker. |

Open PRs: #207 refreshes this WIP snapshot.

## Active Queue

| Priority | Issue | Milestone | Notes |
|---|---|---|---|
| P1 | [#192](https://github.com/bluetape4k/bluetape4k-javers/issues/192) review: conduct repository-wide 7-Tier code review | 0.3.0 | Run before release-prep changes so follow-up defects can be triaged against the current line. |
| P1 | [#208](https://github.com/bluetape4k/bluetape4k-javers/issues/208) fix: make DDD aggregate save audit/event boundary atomic | 0.3.0 | Release-prep data-integrity blocker found by #192. |
| P1 | [#209](https://github.com/bluetape4k/bluetape4k-javers/issues/209) fix: make durable snapshot persist commit-atomic | 0.3.0 | Release-prep data-integrity blocker found by #192. |
| P1 | [#211](https://github.com/bluetape4k/bluetape4k-javers/issues/211) fix: preserve Kafka projection head and sequence semantics | 0.3.0 | Release-prep replay consistency blocker found by #192. |
| P1 | [#212](https://github.com/bluetape4k/bluetape4k-javers/issues/212) fix: restrict javers BOM to publishable modules | 0.3.0 | Release metadata blocker found by #192. |
| P1 | [#213](https://github.com/bluetape4k/bluetape4k-javers/issues/213) fix: align published POM license metadata with MIT | 0.3.0 | Release metadata blocker found by #192. |
| P1 | [#193](https://github.com/bluetape4k/bluetape4k-javers/issues/193) refactor: align remaining data class validation factories | 0.3.0 | Small maintenance/refactor lane; keep behavior locked with targeted tests. |
| P1 | [#194](https://github.com/bluetape4k/bluetape4k-javers/issues/194) refactor: replace deprecated Spring example schema initializer | 0.3.0 | Spring example cleanup; verify affected example tests and deprecation warning removal. |
| P2 | [#210](https://github.com/bluetape4k/bluetape4k-javers/issues/210) fix: define Lettuce repository connection lifecycle | 0.3.0 | Release-prep lifecycle follow-up found by #192. |
| P2 | [#118](https://github.com/bluetape4k/bluetape4k-javers/issues/118) build: move Envers comparison benchmark into a benchmark module | 0.3.0 | Benchmark module registration work; verify Gradle projects/tasks and benchmark docs. |
| P2 | [#195](https://github.com/bluetape4k/bluetape4k-javers/issues/195) build: add benchmark module README and smoke coverage | 0.3.0 | Depends on benchmark module shape from #118. |
| P3 | [#119](https://github.com/bluetape4k/bluetape4k-javers/issues/119) research: evaluate R2DBC persistence support for JaVers snapshots | backlog | Keep separate from 0.3.0; requires backend feasibility research before implementation. |

## Recently Completed

- #77 merged by PR #85: README persistence option diagram.
- #3 merged by PR #86: `javers-exposed` Exposed JDBC CDO snapshot repository.
- #4 merged by PR #87: `javers-ddd` aggregate/domain-event helpers.
- #88 merged by PR #91: command-side `examples/javers-exposed-ddd` scaffold.
- #89 merged by PR #92: Kafka to Redis projection flow.
- #90 merged by PR #93: Envers comparison benchmark results.
- #5 parent is closed by the final tracking PR after #88, #89, and #90 landed.
- #95 merged by PR #96: `javers-exposed` database smoke coverage now uses the
  shared `bluetape4k-exposed-jdbc-tests` H2/PostgreSQL/MySQL_V8 matrix.
- PR #128 opened the `0.3.0` development line.
- PR #206 removed `compileTestKotlin` warning noise.

## Dependency Map

```text
#5 examples/javers-exposed-ddd parent (complete)
  -> #88 command-side Exposed + JaVers + DDD helper flow
      -> #89 Kafka event consumer + Redis projection
          -> #90 Envers comparison benchmark results

#95 javers-exposed DB matrix (complete)
  -> bluetape4k-exposed-jdbc + bluetape4k-exposed-jdbc-tests
  -> H2 + PostgreSQL + MySQL_V8 default shared dialect set

#118 benchmark module move
  -> #195 benchmark README and smoke coverage

#192 release-prep review
  -> #208 DDD aggregate save audit/event consistency
  -> #209 durable snapshot commit atomicity
  -> #210 Lettuce repository lifecycle
  -> #211 Kafka projection replay head/sequence semantics
  -> #212 BOM publishable-module constraints
  -> #213 POM license metadata
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Review / release preflight | 1 | #192 |
| Data integrity blockers | 1 | #208, then #209, then #211 |
| Release metadata blockers | 1 | #213, then #212 |
| Small refactor / maintenance | 1 | #193, then #194 |
| Lifecycle cleanup | 1 | #210 |
| Benchmark build/docs | 1 | #118, then #195 |
| Future research | 1 | #119 only after 0.3.0 is clear |

## Verification Evidence

- Live GitHub issues checked on 2026-06-26 KST:
  `0.3.0` has 11 open issues and `backlog` has 1 open issue.
- Live GitHub PRs checked on 2026-06-26 KST: #207 is open for this WIP update.
- Main worktree checked on 2026-06-26 KST: `develop` is clean and aligned with
  `origin/develop`.
