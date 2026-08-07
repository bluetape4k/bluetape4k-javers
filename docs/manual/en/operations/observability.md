# Observability

Successful API calls do not prove that audit and projection state remain aligned. Observe each boundary separately and correlate it with stable identifiers.

## Signals to collect

- JaVers commit latency and failures, tagged by repository type and aggregate type without exposing sensitive state.
- snapshot encode/decode failures, commit ID, GlobalId, and snapshot version.
- Exposed transaction failures, unique-index conflicts, table growth, and broad-query latency.
- Redis command latency, key count, memory/eviction, persistence health, replication lag, and repository reconstruction failures.
- Kafka send latency/timeouts, producer errors, topic/partition, consumer lag, retry/dead-letter volume, and projection apply failures.
- domain-to-audit and domain-to-projection drift counts from scheduled reconciliation.

`AbstractCdoSnapshotRepository.getAll()` loads all keys and snapshots before filtering and warns when key count exceeds 10,000. Treat that log as a design warning, not only a noisy message. The Kafka repository also warns on every read-path call because reads are unsupported. Alert on such calls during integration testing; they usually mean the wrong persistence role was selected.

## Drift checks

Sample an application aggregate ID, find its latest audit snapshot, and compare the expected business version or fields. Then compare the latest event/projection version in Redis. A mismatch should identify the broken boundary: domain-to-audit, audit-to-publication, consumer lag, or projection apply. Avoid comparing full sensitive payloads in metrics; put detailed evidence in secured diagnostic logs.

The example consumer applies records in poll order and expects per-key Kafka ordering, but it does not expose lag metrics or offsets. Production wiring must add those. See [`OrderProjectionEventConsumer.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderProjectionEventConsumer.kt) and continue with [testing](../guides/testing.md).
