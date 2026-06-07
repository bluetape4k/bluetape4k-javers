# Issue #136 - Vanilla Kafka Snapshot Publisher Plan

## Lane

Full Feature / Type A. The work adds a public repository class, dependency
boundary documentation, Kafka failure/lifecycle behavior, tests, README locale
updates, local reviews, and a PR.

## Tasks

1. Step 0/1 - Setup and requirements
   - Refresh issue #136 with current repository evidence.
   - Create feature worktree from current `origin/develop`.
   - Inspect current Kafka repository, tests, Gradle dependencies, issue #40,
     and sibling `bluetape4k-kafka` helper surfaces.

2. Step 2/3 - Spec and plan review
   - Add the design spec and this plan.
   - Run Step 2-R local 7-tier spec review.
   - Run Step 3-R local plan review.
   - P0=0 and P1=0 required before implementation.

3. Step 4 - Implementation
   - Add `VanillaKafkaCdoSnapshotRepositoryOptions`.
   - Add `VanillaKafkaCdoSnapshotRepository`.
   - Reuse `JaversCodecs.String` and the existing write-only read contract
     pattern from `KafkaCdoSnapshotRepository`.
   - Validate topic and timeout with bluetape4k validation helpers or standard
     `require` when no matching helper exists.
   - Do not add `bluetape4k-kafka` as a mandatory production runtime dependency.

4. Step 5 - Tests and docs
   - Add vanilla repository tests for success capture, failure propagation,
     timeout propagation, interruption status, flush behavior, close ownership,
     validation, and write-only read contract logging.
   - Update `javers-persistence-kafka/README.md`.
   - Update `javers-persistence-kafka/README.ko.md`.

5. Step 6 - Verification
   - Run `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`.
   - Run `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain` and confirm no `spring-kafka` in runtime classpath.
   - Run `git diff --check`.

6. Step 6-R - Final review
   - Add `docs/review/2026-06-08-issue-136-vanilla-kafka-publisher-review.md`.
   - Run local 7-tier final review with P0=0 and P1=0.

7. Step 7/8/9 - PR
   - Add `docs/lessons/2026-06-08-issue-136-vanilla-kafka-publisher.md`.
   - Commit with Lore trailers.
   - Push branch.
   - Create PR assigned to `debop`, milestone `0.3.0`, resolving #136.
   - Use a PR body file, verify live PR body, and ensure the final `##`
     section is `## DoD Status`.
   - Do not merge without explicit user approval.

## Stop Condition

Stop when the PR exists, PR body is verified, validation is recorded, and local
review reports P0=0/P1=0. Merge remains a separate user-approved action.

## Known Risks

- Runtime dependency evidence can be misleading if inspected from
  `testRuntimeClasspath`; use production `runtimeClasspath`.
- Testcontainers-backed Kafka tests must run serially.
- The existing Kafka repository is write-only; tests should not assert read
  behavior beyond the explicit warning/default-return contract.

