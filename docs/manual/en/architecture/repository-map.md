# Repository map

`bluetape4k-javers` owns JaVers audit and history semantics. It does not replace the application repository that owns current business state. Keeping those responsibilities separate makes recovery questions answerable.

| Release module | Responsibility | Not its job |
| --- | --- | --- |
| `javers-core` | codecs, JaVers extensions, cache-backed CDO repositories | durable business storage |
| `javers-ddd` | JaVers-oriented aggregate save/commit/publish sequence | ecosystem-wide DDD contracts |
| `javers-exposed` | JaVers snapshots and commit metadata in SQL | application CRUD repositories |
| `javers-persistence-redis` | Lettuce or Redisson snapshot storage | a Kafka-style replay log |
| `javers-persistence-kafka` | publish encoded snapshots to Kafka | snapshot queries or relational history |
| `examples/javers-exposed-ddd` | executable command/audit/event/projection lesson | a production transaction/outbox design |

The application flow normally starts in its own domain repository. `AggregateRepository.save()` calls the subclass `persist`, then `javers.commit`, then synchronous publishers. The exact order is implemented in [`AggregateRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt).

For repository fundamentals such as JDBC transaction ownership, use the published [bluetape4k-exposed manual](https://bluetape4k.github.io/manual/bluetape4k-exposed/). For Redis, Kafka, and Testcontainers foundations, use the [bluetape4k-projects manual](https://bluetape4k.github.io/manual/bluetape4k-projects/). This manual covers how those facilities participate in a JaVers audit path.

Release 0.2.1 has no composite `CdoSnapshotRepository` that automatically writes to Exposed and Redis or couples Kafka with a query repository. Composition is an application responsibility. Read [repository composition](repository-composition.md) before adding a second destination.
