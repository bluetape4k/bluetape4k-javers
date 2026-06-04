# Issue 110 Stale Snapshot Identifier Guard

## Context

`AbstractCdoSnapshotRepository.getSnapshots(snapshotIdentifiers)` used list-index
math to resolve a requested snapshot version. That assumes a dense, gap-free
snapshot list. A stale or partially missing persisted version could point at the
wrong snapshot or throw an out-of-bounds exception.

## Decision

Resolve snapshot identifiers by exact `CdoSnapshot.version` match after loading
the target global id history. Keep missing/stale identifiers out of the returned
subset instead of throwing.

## Outcome

Valid identifiers still return the expected persisted snapshot, while stale
positive identifiers are ignored. The shared repository contract now simulates a
version hole through a test-only repository.

## Verification

- `./gradlew :javers-core:test --tests "org.javers.repository.jql.InMemoryJaversShadowTest" --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`

## Future Guidance

Do not map JaVers snapshot versions to list indexes unless the storage contract
explicitly guarantees a dense version sequence. Prefer exact version matching
when callers provide `SnapshotIdentifier` values.
