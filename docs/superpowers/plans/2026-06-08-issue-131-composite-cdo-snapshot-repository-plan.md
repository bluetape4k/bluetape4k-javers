# Issue #131 - Composite CDO Snapshot Repository Plan

## Lane

Full Feature / Type A.

The work adds a public repository composition API, failure-policy model, tests,
README locale updates, local 7-Tier reviews, and a PR for milestone `0.3.0`.

## Step 0/1 Evidence

- Worktree: `.worktrees/feat-issue-131-composite-repository`.
- Base: `origin/develop@e54336d`.
- Live issue #131 body refreshed after #105 / PR #185 merge.
- Current contracts reviewed:
  - `CdoSnapshotRepository`
  - `AbstractCdoSnapshotRepository`
  - Kafka write-only repositories
  - `KafkaCdoSnapshotProjector`
  - #133 Redis + Exposed latency strategy
- GNO `bluetape4k-wiki` system-design query returned no direct match for this
  composite/fanout topic.

## Implementation Tasks

1. Reconfirm Kotlin guidance before source edits.
   - Reload `bluetape4k-code-patterns`.
   - Run CodeGraph impact/context check for the planned new core files and
     existing `CdoSnapshotRepository` surface.
   - Confirm no existing generic composite repository exists in `javers-core`.

2. Add composite failure model in `javers-core`.
   - Add `CompositeCdoSnapshotFailurePolicy`.
   - Add `CompositeCdoSnapshotDelegateKind`.
   - Add `CompositeCdoSnapshotWriteFailure`.
   - Add `CompositeCdoSnapshotException`.
   - Use English KDoc and bluetape4k validation helpers.
   - Keep serializable data classes with `serialVersionUID`.

3. Add composite options.
   - Add `CompositeCdoSnapshotRepositoryOptions`.
   - Defaults:
     - `writeFailurePolicy = FAIL_FAST`
     - `ensureSchemaFailurePolicy = FAIL_FAST`
     - `closeFailurePolicy = BEST_EFFORT`
   - Use private constructor plus companion `operator fun invoke(...)` if
     validation or invariant enforcement is needed.

4. Add `CompositeCdoSnapshotRepository`.
   - Implement `CdoSnapshotRepository` directly.
   - Delegate public read methods and `getHeadId()` to primary.
   - Propagate `setJsonConverter()` to primary and secondaries.
   - Propagate `ensureSchema()` primary first, then secondaries according to
     options.
   - `saveSnapshot()` writes primary first, then ordered secondaries.
   - `persist(commit)` calls `primary.persist(commit)` first, then ordered
     secondaries, so the primary repository keeps its native head/sequence
     behavior.
   - Primary failure always prevents secondary writes.
   - `FAIL_FAST` stops on first secondary failure.
   - `BEST_EFFORT` attempts all secondary writes, then throws aggregate failure.
   - `close()` attempts every closeable delegate and reports aggregate failures.

5. Add core tests.
   - Options defaults.
   - Read delegation for representative Javers read methods plus `loadSnapshots`
     and `getHeadId`.
   - `setJsonConverter()` propagation.
   - `ensureSchema()` propagation and failure policy.
   - Primary-before-secondary write order.
   - Primary failure prevents secondary saves.
   - `FAIL_FAST` secondary failure stops later secondaries.
   - `BEST_EFFORT` secondary failure attempts all secondaries and aggregates.
   - `close()` attempts every closeable and aggregates close failures.
   - JaVers commit with Caffeine primary and recording secondaries.

6. Update docs.
   - Update `javers-core/README.md`.
   - Update `javers-core/README.ko.md`.
   - Include recommended Exposed + Kafka and Exposed + Redis + Kafka shapes.
   - State non-atomicity, failure policies, and Kafka write-only boundary.
   - Update root README locale pair only if the root module overview needs a
     short cross-reference.

7. Add review and lesson artifacts.
   - Add `docs/review/2026-06-08-issue-131-composite-cdo-snapshot-repository-review.md`.
   - Add `docs/lessons/2026-06-08-issue-131-composite-cdo-snapshot-repository.md`.

## Verification Tasks

1. Static/pattern scan of touched Kotlin:
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
   - Commit with Lore trailers.
   - Push branch.
   - Create PR assigned to `debop`, milestone `0.3.0`, resolving #131.
   - Use `--body-file`, verify live PR body, and ensure final section is
     `## DoD Status`.
   - Do not merge without explicit user approval.

## Rejected Alternatives

- Put the composite in `javers-persistence-kafka`: rejected because Exposed,
  Redis, Caffeine, and Kafka should all compose through the shared
  `CdoSnapshotRepository` contract without module dependency cycles.
- Extend `AbstractCdoSnapshotRepository`: rejected because the composite does
  not own codec serialization, commit sequence storage, or head restoration.
- Add a new JaVers cache abstraction: rejected by #131 and #133 reuse
  constraints.
- Add automatic retry/outbox/compensation: rejected because that changes
  reliability and operational semantics beyond this issue.
- Roll back primary storage on secondary failure: rejected because existing
  `CdoSnapshotRepository` implementations do not expose a safe rollback API and
  #131 explicitly avoids distributed transaction semantics.
- Make Kafka repositories read-capable: rejected because #105 already added an
  explicit projector while preserving Kafka write-only repositories.

## Stop Condition

Stop when the PR exists, live PR body is verified, local validation passes,
Step 6-R reports P0=0/P1=0, and CI status is ready for user-approved merge.

Merge remains a separate user-approved action.
