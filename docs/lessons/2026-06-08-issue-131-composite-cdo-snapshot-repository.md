# Issue 131 Composite CDO Snapshot Repository

## Context

Issue #131 added a direct composite JaVers `CdoSnapshotRepository` for one read/query primary plus ordered secondary fanout. The important design correction was that `persist(commit)` must call `primary.persist(commit)` directly, not only fan out snapshots through `saveSnapshot()`, because the primary repository owns its native head and sequence semantics.

## Decision

- Keep the composite additive under `javers-core`.
- Delegate all reads to the primary repository.
- Write primary first, then secondaries in order.
- Do not promise atomicity across delegates. If a secondary fails after primary success, surface the failure without rolling back the primary.
- Reject rollback/distributed transaction behavior because `CdoSnapshotRepository` does not expose a safe rollback contract.

## Outcome

The implementation introduced explicit failure policies, delegate failure metadata, aggregate exceptions, primary-first write behavior, README locale updates, and an English-label README diagram. Review also tightened `CompositeCdoSnapshotException` so it rejects empty failure lists before payload construction and stores a defensive copy.

## Verification

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`
  - `BUILD SUCCESSFUL`
  - `SUCCESS: Executed 197 tests in 13.8s`
- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - `BUILD SUCCESS`
  - `SUCCESS: Executed 39 tests in 12.8s`
- Static forbidden-pattern scans returned no matches.
- README diagram PNG was rendered and visually inspected.

## Future Guard

For future composite repository work, review the JaVers repository head/sequence contract before choosing inheritance or direct delegation. If secondary writes can fail after primary success, document the non-atomic consistency semantics in KDoc, README, and tests instead of implying rollback behavior.
