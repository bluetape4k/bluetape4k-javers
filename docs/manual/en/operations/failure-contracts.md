# Failure contracts

The 0.2.1 adapters propagate storage and publication failures, but propagation is not rollback. Define what remains after each step before adding retries.

| Failure point | Possible state | Required production response |
| --- | --- | --- |
| domain persistence | no audit or event | normal command retry policy |
| JaVers snapshot/commit sequence | domain state may already exist; partial audit writes are possible | reconcile by aggregate, GlobalId, version, and commit ID |
| Kafka publication | domain and audit may exist without an event | outbox/retry record; do not blindly repeat the whole command |
| consumer/projection | Kafka record may be replayed; Redis may be stale or partly updated | idempotent apply, controlled offsets, replay and drift repair |

`AbstractCdoSnapshotRepository.persist` saves snapshots one at a time, then advances the in-memory head and writes a commit sequence. A thrown exception stops later work; it does not undo already completed external writes. The implementation is in [`AbstractCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt).

Idempotency is an application contract in 0.2.1. SQL has a unique GlobalId/version index, Kafka uses GlobalId as a key, and the example uses deterministic Redis keys, but none supplies a complete command/event deduplication protocol. Record stable command and event IDs, decide whether duplicate commits are allowed, and make projection updates compare an event sequence or version.

Schema ownership also belongs to deployment. `ensureSchema()` is convenient, not a migration ledger. Kafka topic retention/partitioning and Redis persistence/eviction are equally part of correctness. Document owners and recovery commands alongside the service.

Continue with [observability](observability.md) to turn these states into signals.

