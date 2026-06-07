# Issue #133 - Redis + Exposed Latency Strategy Plan

## Lane

Full Feature / Type A, constrained to strategy documentation and validation.
The issue touches cross-module behavior, Redis, Exposed, and performance
strategy, but current evidence does not justify a new production repository.

## Tasks

1. Spec/plan gate
   - Refresh issue #133 with current merged evidence.
   - Confirm current `javers-exposed`, `javers-persistence-redis`, and sibling
     `bluetape4k-exposed` cache contracts.
   - Write the design spec and this execution plan.
   - Run local Step 2-R and Step 3-R review with P0=0/P1=0 before PR.

2. Public documentation
   - Update `javers-exposed/README.md`.
   - Update `javers-exposed/README.ko.md`.
   - Update `javers-persistence-redis/README.md`.
   - Update `javers-persistence-redis/README.ko.md`.
   - Keep English/Korean locale parity.

3. Tests and validation
   - Run `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`.
   - Run `git diff --check`.
   - Record why no new JaVers-specific cache tests were added: no new runtime
     behavior or production cache mapping code is introduced in this issue.

4. Review and lesson
   - Add `docs/review/2026-06-07-issue-133-redis-exposed-latency-strategy-review.md`.
   - Add `docs/lessons/2026-06-07-issue-133-redis-exposed-latency-strategy.md`.
   - Review must include P0/P1/P2/P3 counts and explicit P0=0/P1=0 verdict.

5. PR
   - Commit with Lore trailers.
   - Push branch.
   - Create a PR assigned to `debop`, milestone `0.3.0`, resolving #133.
   - Use a PR body file and verify the live PR body.
   - The final PR body section must be `## DoD Status`.

## Stop Condition

Stop when the branch is pushed, the PR exists with verified body, local targeted
validation has passed, and the local review reports P0=0 and P1=0. Do not merge
without explicit user approval.

## Known Risks

- Users may expect a new Redisson near-cache JaVers repository. The README must
  state that existing Exposed cache modules are the supported reuse path for
  read models and projections.
- Write-behind must not be presented as safe for canonical audit writes.
- Generic cache test coverage belongs in `bluetape4k-exposed`; this repository
  should only add JaVers-specific tests when JaVers-specific mapping code exists.

