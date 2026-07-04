# Issue #211 Kafka projection head and sequence semantics

## Context

`KafkaCdoSnapshotProjector` decoded Kafka records and called
`saveSnapshot(snapshot)` directly. That rebuilt snapshot rows but bypassed the
repository path that maintains commit head and sequence metadata, so replayed
repositories could be readable while reporting stale or missing head state.

## Decision

Add `CdoSnapshotRepository.projectSnapshot` as the replay contract. The default
method remains snapshot-only for simple custom repositories, while
`AbstractCdoSnapshotRepository` restores snapshot data, commit sequence, and
head state. Exposed and Lettuce override the projection persistence hook so the
snapshot and sequence update share the backend's transaction or `MULTI`/`EXEC`
boundary.

## Outcome

Kafka projection now restores `getHeadId()` and query ordering for core,
Exposed, and Lettuce Redis projection targets. Offset commits remain after
successful projection, and decode/projection failures still prevent offset
commit.

## Future Guidance

Projectors should call `projectSnapshot`, not `saveSnapshot`, when replaying
decoded audit records. New durable repositories should inherit
`AbstractCdoSnapshotRepository` or override `projectSnapshot` with equivalent
head and sequence restoration semantics.
