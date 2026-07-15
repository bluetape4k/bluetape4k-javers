# Redis persistence

Redis is useful when JaVers snapshot access must use an existing Redis estate, but it is not automatically a durable event log. Retention, persistence mode, replication, backup, failover, and eviction policy determine whether Redis history survives an incident.

Release 0.2.1 provides two repositories:

- `LettuceCdoSnapshotRepository(name, RedisClient)` uses dedicated synchronous commands. A Redis `MULTI/EXEC` transaction updates the snapshot list and GlobalId index together; the commit-sequence `HSET` is separate. A dedicated write connection avoids shared-connection races.
- `RedissonCdoSnapshotRepository(name, RedissonClient)` uses Redisson list-multimap and map structures with configured codecs.

Both store encoded snapshots newest first per GlobalId, keep commit sequence data separately, and restore the latest head after repository reconstruction. They implement JaVers reads; unlike the Kafka adapter, they can load snapshot history and shadows. Their exact structures are in [`LettuceCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt) and [`RedissonCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt).

## Selection and recovery

Choose Lettuce when the service already manages Lettuce clients and wants explicit command-level behavior. Choose Redisson when Redisson distributed objects and lifecycle are the established operational path. Do not run both against the same namespace unless their wire structures have been proven compatible; 0.2.1 does not document cross-client migration.

Redis command failures propagate. A retry can encounter partially updated structures, so verify by commit ID and snapshot version rather than assuming the first attempt had no effect. Broad JQL queries enumerate keys and decode values in the process; large histories need memory and latency monitoring.

Use the [Projects Redis manual](https://bluetape4k.github.io/manual/bluetape4k-projects/1.11/modules/bluetape4k-redis/) for client lifecycle and topology. Use [observability](../operations/observability.md) for audit-specific signals.
