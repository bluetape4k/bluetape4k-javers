# Issue #138 — Exposed EntityHook JaVers Audit Plan

## Lane

Full Feature / Type A: public API, new DAO dependency surface, transaction-bound
Exposed behavior, README locale updates, local review gate.

## Tasks

1. Spec/plan gate
   - Refresh issue body with current dependency evidence.
   - Confirm Exposed and JaVers source API from local source jars.
   - Write this spec and plan.

2. Dependency and API implementation
   - Add `exposed-dao` alias/dependency to `javers-exposed`.
   - Add `ExposedJaversEntityHookMapping`.
   - Add `ExposedJaversEntityHookSubscription`.
   - Use bluetape4k validation helpers and English KDoc.

3. Tests
   - Add H2 DAO table/entity/test model.
   - Cover create, update, delete, final-event-only coalescing, rollback, and close/unsubscribe.
   - Keep Testcontainers/DB verification serial.

4. README and lesson
   - Update `javers-exposed/README.md`.
   - Update `javers-exposed/README.ko.md`.
   - Add concise lesson under `docs/lessons/`.

5. Verification
   - Compile touched module.
   - Run targeted javers-exposed and javers-ddd tests serially.
   - Run `git diff --check`.

6. Review and PR
   - Create tracked `docs/review/2026-06-07-issue-138-exposed-entityhook-audit-review.md`.
   - Local review must report P0=0 and P1=0 before PR.
   - Commit with Lore trailers.
   - Create PR assigned to `debop`, milestone `0.3.0`, resolving #138.
   - Verify live PR body and final `## DoD Status` section.

## Stop Condition

Stop when implementation is committed, PR is created, PR body is verified, and
local review has P0=0/P1=0. Do not merge without explicit user request.

## Known Risks

- `EntityHook` is global, so missed `close()` calls can leak subscribers.
- Exposed DAO lifecycle does not cover DSL writes; README must not overclaim.
- JaVers terminal delete uses local id/type only, not full deleted object state.
