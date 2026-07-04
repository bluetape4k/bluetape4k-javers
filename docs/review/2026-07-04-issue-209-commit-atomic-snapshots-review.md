# Issue #209 commit-atomic snapshot persistence review

## Scope

- Introduce a repository hook that writes all snapshots and commit sequence metadata as one commit unit.
- Wrap Exposed snapshot persistence in a single database transaction.
- Batch Lettuce Redis snapshot writes and sequence metadata in one `MULTI`/`EXEC` boundary.
- Document Redisson's best-effort boundary because its current data structures do not provide the same commit-level atomicity.
- Add failure-injection coverage for multi-snapshot Exposed commits.

## 7-Tier Review

| Tier | Verdict | Evidence |
|---|---|---|
| 1. Correctness | PASS | `persist` now reserves one sequence and delegates snapshots plus sequence metadata to `persistCommit`; `head` advances only after that call returns. |
| 2. API and compatibility | PASS | The new `persistCommit` hook is `protected open`, preserving the public repository API. |
| 3. Atomicity semantics | PASS | Exposed wraps the whole commit in `inTransaction`; Lettuce queues all snapshot writes and the sequence update before `exec()`. |
| 4. Failure behavior | PASS | The Exposed regression test fails after the first snapshot encode and verifies zero snapshot rows, zero commit rows, and no head id. |
| 5. Redis consistency | PASS | Lettuce encodes snapshots before `MULTI` and serializes `MULTI`/`EXEC` with the existing transaction lock; existing Redis parity tests remain green. |
| 6. Documentation/KDoc | PASS | Core and Lettuce KDoc describe commit-level persistence; Redisson KDoc explicitly documents the best-effort limitation. |
| 7. Release maintainability | PASS | The hook centralizes future backend-specific atomicity without changing JaVers-facing repository usage. |

## Validation

- `./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 191 tests.
- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryH2Test*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 9 tests.
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 76 tests.
- `git diff --check`
  - Result: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
