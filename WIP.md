# WIP - bluetape4k-javers

Snapshot: 2026-06-26 KST
Scope: open GitHub issues assigned to `debop`.
Open count: 5 issues.

## Current Direction

`0.2.0` is the latest published stable Javers release and is the version
consumed by `bluetape4k-dependencies` `1.2.0`.

The `0.2.1` patch lane has no open GitHub issues, but it is not tagged or
published yet. Treat `0.2.1` as the stable Javers candidate for the next
`bluetape4k-dependencies` release train until the release is completed and
Maven Central visibility is verified.

Development is open on the `0.3.0` line with `snapshotVersion=` kept empty for
workflow-injected snapshot publication. `0.3.0` is not the stable Javers input
for the next dependencies release train unless the train is explicitly retargeted.

## Milestone Snapshot

| Milestone | Open | Closed | Notes |
|---|---:|---:|---|
| `0.2.1` | 0 | 21 | Patch candidate for the next dependencies train; tag/release still missing. |
| `0.3.0` | 4 | 77 | Next development queue after the 0.2.1 patch lane. |
| `backlog` | 1 | 4 | Future R2DBC persistence research; not a `0.3.0` blocker. |

Open PRs: none.

## Active Queue

| Priority | Issue | Milestone | Notes |
|---|---|---|---|
| P1 | [#208](https://github.com/bluetape4k/bluetape4k-javers/issues/208) fix: make DDD aggregate save audit/event boundary atomic | 0.3.0 | Release-prep data-integrity blocker found by #192. |
| P1 | [#209](https://github.com/bluetape4k/bluetape4k-javers/issues/209) fix: make durable snapshot persist commit-atomic | 0.3.0 | Release-prep data-integrity blocker found by #192. |
| P1 | [#211](https://github.com/bluetape4k/bluetape4k-javers/issues/211) fix: preserve Kafka projection head and sequence semantics | 0.3.0 | Release-prep replay consistency blocker found by #192. |
| P2 | [#118](https://github.com/bluetape4k/bluetape4k-javers/issues/118) build: move Envers comparison benchmark into a benchmark module | 0.3.0 | Benchmark module registration work; verify Gradle projects/tasks and benchmark docs. |
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
- #192, #193, #194, #210, #212, and #213 are closed on the `0.3.0` line.
- #195 merged by PR #219: benchmark module README and CI/Nightly smoke coverage.

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
| Data integrity blockers | 1 | #208, then #209, then #211 |
| Release metadata blockers | 1 | Complete for the current WIP snapshot. |
| Small refactor / maintenance | 1 | Complete for the current WIP snapshot. |
| Lifecycle cleanup | 1 | Complete for the current WIP snapshot. |
| Benchmark build/docs | 1 | #118 |
| Future research | 1 | #119 only after 0.3.0 is clear |

## Verification Evidence

- Live GitHub issues checked on 2026-06-26 KST:
  `0.2.1` has 0 open issues, `0.3.0` has 4 open issues, and `backlog` has 1
  open issue.
- Live GitHub releases checked on 2026-06-26 KST: `0.2.0` exists; `0.2.1` does
  not yet exist.
- Live GitHub PRs checked on 2026-06-26 KST: no open PRs.
- Main worktree checked on 2026-06-26 KST: `develop` is clean and aligned with
  `origin/develop`.
