# Repository composition

Applications often want durable SQL history, fast reads, and event publication at once. Release 0.3.0 does not provide a composite snapshot repository that makes those three writes atomic. JaVers registers one `JaversRepository`; any second destination introduces an application-level consistency boundary.

The cache-backed repositories in `javers-core`—Caffeine, Cache2k, and JCache—are complete in-memory `CdoSnapshotRepository` implementations. They insert snapshots newest first and keep commit ordering in a separate cache. They are useful for tests and process-local history, but eviction or restart can remove data. See [`CaffeineCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/caffeine/CaffeineCdoSnapshotRepository.kt).

`CompositeDomainEventPublisher` is different: it forwards one domain event to publishers in list order. It stops on the first exception and does not roll back earlier publishers. Its contract is in [`DomainEventPublisher.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEventPublisher.kt). Do not infer a composite persistence guarantee from this publisher.

## Safe composition rule

Choose one audit source that can answer required history queries. Add Kafka as publication, not as a query replacement. Build Redis projections from durable events only when the system also owns replay, deduplication, offset handling, and drift repair. If SQL and Redis are both written directly, define which one wins after partial failure and provide a reconciliation job.

The 0.3.0 DDD example deliberately demonstrates order, not atomicity: application persistence, JaVers commit, event publication, then consumer-side Redis projection. The [DDD/CQRS guide](../guides/ddd-and-cqrs.md) explains where an outbox and idempotent consumer belong in production.
