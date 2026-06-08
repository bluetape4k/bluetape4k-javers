# Issue #105 - Kafka Audit Projection Plan

## Lane

Full Feature / Type A. The work adds a public projection API, replay semantics,
Kafka/Redis integration coverage, README locale updates, local reviews, and a
PR.

## Tasks

1. Step 0/1 - Setup and requirements
   - Refresh issue #105 with current repository evidence.
   - Create feature worktree from current `origin/develop`.
   - Inspect current Kafka publishers, #89 example projection, Redis repository
     target, and bluetape4k-kafka helpers.

2. Step 2/3 - Spec and plan review
   - Add the design spec and this plan.
   - Check that the plan preserves write-only Kafka repository behavior.
   - Check that the target read store is an existing `CdoSnapshotRepository`.
   - P0=0 and P1=0 required before implementation.

3. Step 4 - Implementation
   - Add `KafkaCdoSnapshotProjectionOptions`.
   - Add `KafkaCdoSnapshotProjectionResult`.
   - Add `KafkaCdoSnapshotProjector`.
   - Reuse `bluetape4k-kafka` `consumerOf(...)` for repository-owned consumer
     convenience constructors.
   - Reuse existing `JaversCodecs.String` and JaVers `JsonConverter` for the
     current wire value.
   - Do not change Kafka repository read methods.

4. Step 5 - Tests and docs
   - Add unit tests for validation, duplicate skip, failure offset behavior, and
     consumer ownership.
   - Add Kafka Testcontainers integration that projects to
     `LettuceCdoSnapshotRepository`.
   - Update `javers-persistence-kafka/README.md`.
   - Update `javers-persistence-kafka/README.ko.md`.

5. Step 6 - Verification
   - Run `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`.
   - Run `git diff --check`.

6. Step 6-R - Final review
   - Add `docs/review/2026-06-08-issue-105-kafka-audit-projection-review.md`.
   - Run local 7-tier final review with P0=0 and P1=0.

7. Step 7/8/9 - PR
   - Add a short lesson if implementation reveals durable workflow guidance.
   - Commit with Lore trailers.
   - Push branch.
   - Create PR assigned to `debop`, milestone `0.3.0`, resolving #105.
   - Use a PR body file, verify live PR body, and ensure the final `##`
     section is `## DoD Status`.
   - Do not merge without explicit user approval.

## Stop Condition

Stop when the PR exists, PR body is verified, validation is recorded, and local
review reports P0=0/P1=0. Merge remains a separate user-approved action.

## Known Risks

- Kafka and Redis Testcontainers must run serially.
- The current Kafka wire value is still the encoded snapshot payload only.
- Replay is idempotent for exact duplicate snapshots already present in the
  target repository, not an exactly-once Kafka transaction guarantee.
