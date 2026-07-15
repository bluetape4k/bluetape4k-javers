# Persistence selection guide

Pick a repository by the question it must answer after a failure, not by latency alone.

| Concern | Exposed | Redis | Kafka repository |
| --- | --- | --- | --- |
| Primary role | durable SQL audit history | Redis-backed snapshot history | snapshot publication stream |
| Query shape | JaVers queries, implemented partly by in-memory filtering | JaVers queries after scanning Redis keys/lists | no reads; methods return empty/default |
| Ordering | snapshot version plus stored commit sequence | list order plus stored commit sequence | producer/partition ordering only; no repository reads |
| Replay | database restore/query, not an event replay engine | restore from Redis persistence or rebuild externally | consumers may replay retained records if configured |
| Cache behavior | none in the adapter | Redis is the repository, not a near-cache | none |
| Recovery owner | database/schema operator | Redis persistence/backup owner | Kafka producer, topic, consumer, and projection owners |

Use [Exposed](exposed.md) when audit history must survive process restart and fit database operations. Use [Redis](redis.md) when Redis is an accepted snapshot store and the team owns its durability configuration. Use [Kafka](kafka.md) only when publishing encoded snapshots is the goal and another system owns consumption and query state.

None of these adapters promises exactly-once end-to-end behavior. Exposed operations use separate transactions, Redis updates cover several structures, and Kafka waits up to 30 seconds for a send result. Plan retry and reconciliation around identifiers such as GlobalId, snapshot version, commit ID, aggregate ID, and consumer offset.

For mixed designs, read [repository composition](../architecture/repository-composition.md) and [failure contracts](../operations/failure-contracts.md).
