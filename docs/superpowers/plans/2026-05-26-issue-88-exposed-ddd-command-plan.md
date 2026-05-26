# Issue #88 — javers-exposed-ddd Command-Side Example Plan

Date: 2026-05-26
Issue: https://github.com/bluetape4k/bluetape4k-javers/issues/88
Spec: `docs/superpowers/specs/2026-05-26-issue-88-exposed-ddd-command-design.md`

## Work Type

Type A — Full Design. This adds a new example module, CI/Nightly coverage, and
user-facing README updates.

## Tasks

1. Module registration
   - Add `javers-exposed-ddd` to `settings.gradle.kts`.
   - Add `javers-exposed-ddd/build.gradle.kts`.
   - Use H2 for tests; defer Kafka/Redis dependencies to #89.

2. Command-side domain model
   - Add order ID/customer ID/item/status value types.
   - Add `Order` aggregate with JaVers `@Id`.
   - Add command and domain event types.

3. Exposed persistence
   - Add `OrdersTable`.
   - Add repository implementation backed by Exposed JDBC.
   - Use JSON for line items in this first slice.

4. Command handler
   - Add `OrderCommandHandler`.
   - Save aggregate state through `AggregateRepository`.
   - Publish one domain event per command.

5. Tests
   - H2-backed test fixture creates order and JaVers tables.
   - Verify persisted aggregate, JaVers snapshot history, commit properties,
     and published events.

6. Documentation and workflow coverage
   - Add module README.md and README.ko.md.
   - Update root README.md and README.ko.md.
   - Update WIP.md and add a lesson file.
   - Update CI and Nightly path filters/jobs for the example module.

7. Verification
   - `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `actionlint`
   - `git diff --check`
   - GitHub PR checks after PR creation.

## PR Boundary

This PR closes #88 only and references #5 as the parent. It must not close #5,
#89, or #90.

## Open Constraints

- Claude advisor review may be unavailable due disabled Claude organization.
  If unavailable, preserve the artifact and note the gap in the PR.
- Testcontainers are intentionally not used in #88. #89 will add Kafka/Redis
  integration tests and must run those serially.
