# Issue #211 Kafka projection head and sequence review

## Scope

- Define a replay-specific `projectSnapshot` repository contract.
- Restore commit head and sequence metadata when Kafka projection replays decoded snapshots into `AbstractCdoSnapshotRepository` implementations.
- Keep Exposed projection snapshot and sequence writes in one database transaction.
- Keep Lettuce Redis projection snapshot and sequence writes in one serialized `MULTI`/`EXEC` boundary.
- Add regression coverage for Caffeine, Exposed H2, and Lettuce Redis projection targets.

## 7-Tier Review

| Tier | Verdict | Evidence |
|---|---|---|
| 1. Correctness | PASS | `KafkaCdoSnapshotProjector` now calls `projectSnapshot`, which restores snapshot rows plus commit head/sequence metadata for `AbstractCdoSnapshotRepository` implementations. |
| 2. API and compatibility | PASS | `CdoSnapshotRepository.projectSnapshot` has a default snapshot-only implementation, so existing custom implementations remain source-compatible. |
| 3. Head/sequence semantics | PASS | `AbstractCdoSnapshotRepository.projectSnapshot` reuses an existing sequence for duplicate commit ids and only advances head when the projected sequence is newer or equal. |
| 4. Backend atomicity | PASS | Exposed wraps projection writes in `inTransaction`; Lettuce wraps snapshot/index/sequence writes in one `MULTI`/`EXEC`; Redisson best-effort limitations are documented. |
| 5. Kafka offset safety | PASS | Offset commits still happen after the full polled batch projects successfully; decode/projection failures still skip `commitSync()`. |
| 6. Tests and silent failure | PASS | Caffeine, Exposed H2, and Lettuce Redis tests assert `getHeadId()` and newest-first replay ordering. |
| 7. Release maintainability | PASS | Projection contract is centralized in `CdoSnapshotRepository`, keeping future projectors and repositories on one replay API. |

## Validation

- `./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-kafka:compileKotlin :javers-persistence-kafka:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
- `./gradlew :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotProjector*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 9 tests.
- `./gradlew :javers-core:test :javers-persistence-kafka:test :javers-exposed:test :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
  - `javers-core`: 191 tests, 0 failures.
  - `javers-persistence-kafka`: 43 tests, 0 failures.
  - `javers-exposed`: 54 tests, 0 failures.
  - `javers-persistence-redis`: 76 tests, 0 failures.
- `git diff --check`
  - Result: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
