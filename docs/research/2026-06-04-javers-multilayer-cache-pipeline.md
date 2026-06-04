---
title: JaVers multilayer repository cache and pipeline research
date: 2026-06-04
source_type: official-docs-plus-repo-check
repo: bluetape4k/bluetape4k-javers
---

# JaVers multilayer repository cache and pipeline research

## Source links

- Redisson collections reference: https://redisson.pro/docs/data-and-services/collections/
  - PullMD: quality 1, share_id `ef7a9374`, fetched `2026-06-04T10:17:52.307Z`.
- Redis cache-aside guide: https://redis.io/docs/latest/develop/use-cases/cache-aside/
  - PullMD: quality 1, share_id `d2a51402`, fetched `2026-06-04T10:17:47.584Z`.
- Lettuce reference guide: https://redis.github.io/lettuce/
  - PullMD: quality 1, share_id `4f92062d`, fetched `2026-06-04T10:19:12.269Z`.
- Apache Kafka `KafkaProducer` API: https://kafka.apache.org/40/javadoc/org/apache/kafka/clients/producer/KafkaProducer
  - PullMD: quality 0.85, share_id `665e729d`, fetched `2026-06-04T10:19:12.307Z`.
- Spring Kafka sending messages: https://docs.enterprise.spring.io/spring-kafka/reference/3.1/kafka/sending-messages.html
  - PullMD: quality 1, share_id `286bbdcf`, fetched `2026-06-04T10:19:23.091Z`.
- NATS JetStream publishing: https://docs.nats.io/using-nats/developer/develop_jetstream/publish
  - PullMD: quality 1, share_id `a4ef01a2`, fetched `2026-06-04T10:19:12.065Z`.
- AWS SDK for Java `SqsClient`: https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/sqs/SqsClient.html
  - PullMD: quality 1, share_id `0e244361`, fetched `2026-06-04T10:19:12.174Z`.

## Repo-local facts

- `bluetape4k-javers` already has `javers-exposed`, `javers-persistence-redis`, and `javers-persistence-kafka`.
- `javers-persistence-redis` already provides both `RedissonCdoSnapshotRepository` and `LettuceCdoSnapshotRepository`.
- `javers-persistence-kafka` currently uses Spring Kafka `KafkaTemplate` and is explicitly write-only.
- `bluetape4k-projects/cache/cache-core` already provides provider-neutral `NearCacheOperations`, `SuspendNearCacheOperations`, resilience decorators, local cache providers such as Caffeine, and shared near-cache test guidance.
- `bluetape4k-projects/cache/cache-lettuce` already provides Lettuce native/JCache near-cache implementations, RESP3 client-tracking invalidation, resilient near-cache variants, and benchmark evidence.
- `bluetape4k-projects/cache/cache-redisson` already provides Redisson JCache, `RedissonNearCache`, `RedissonSuspendNearCache`, `RLocalCachedMap`-backed near-cache support, and memoizers.
- `bluetape4k-exposed/exposed/exposed-cache` already defines cache-backend-agnostic Exposed repository contracts, `CacheMode`, `CacheWriteMode`, `LocalCacheConfig`, Redis-specific repository contracts, and reusable test fixtures.
- `bluetape4k-exposed/exposed/exposed-jdbc-lettuce` already implements Exposed JDBC + Lettuce Redis read-through, write-through, write-behind, synchronous and suspended repository contracts, and Exposed-based `MapLoader` / `MapWriter`.
- `bluetape4k-exposed/exposed/exposed-jdbc-redisson` already implements Exposed JDBC + Redisson read-through, write-through, write-behind, near-cache, synchronous and suspended repository contracts, and Exposed-based `MapLoader` / `MapWriter`.
- JDBC-backed Exposed repositories are the preferred high-throughput baseline when they can run on Java virtual threads: the blocking JDBC path stays simple, while virtual threads reduce request/thread contention. JaVers JDBC cache code should therefore avoid monitor-based synchronization and preserve virtual-thread friendliness.
- Issue #89 / PR #92 already added an example CQRS flow: Exposed command-side snapshots, Kafka events, and Redis projection.
- Issue #105 is an open design issue for a read-capable Kafka audit projection path.
- Issue #131 is an open composite repository issue for durable history plus event stream. This research updated it to milestone `0.3.0`.

## Research synthesis

Redis cache-aside remains the conservative baseline for lowering repeated read latency: read Redis first, fall back to the database, write the loaded value to Redis, and invalidate on primary writes. Redis documents TTL-bounded staleness, explicit invalidation, hashes/JSON for partial updates, and Lua-based stampede mitigation as useful pieces.

Redisson gives the stronger feature set for a true repository-level cache layer. Its `RMap` family supports read-through via `MapLoader`, write-through via synchronous `MapWriter`, and write-behind via delayed batched `MapWriter`. `RLocalCachedMap` adds a near-cache on the client side, with invalidation/update synchronization across instances. This is directly relevant to a Redis + Exposed + DB latency layer, but the implementation must make staleness, replay/rebuild, and failure semantics explicit.

Lettuce is a good low-level Redis client for synchronous, asynchronous, and reactive access. It is already present in the JaVers Redis repository, so new work should focus on parity tests, selection guidance, and avoiding transactional connection misuse rather than creating a duplicate repository.

For implementation planning, the JaVers work should not create a fresh cache/exposed abstraction from scratch. The required path is to reuse and adapt the existing bluetape4k cache and Exposed cache surfaces:

- Use `bluetape4k-projects/cache/cache-core` contracts for provider-neutral near-cache behavior, statistics, resilience, and test expectations.
- Use `bluetape4k-projects/cache/cache-lettuce` and `cache/cache-redisson` provider implementations before adding any JaVers-specific near-cache wrapper.
- Use `bluetape4k-exposed/exposed/exposed-cache` contracts and test fixtures for read-through, write-through, write-behind, `CacheMode`, and `CacheWriteMode` semantics.
- Use `bluetape4k-exposed/exposed/exposed-jdbc-lettuce` and `exposed/exposed-jdbc-redisson` repository implementations as the baseline for Exposed + Redis + DB behavior, especially their `MapLoader` / `MapWriter`, sync/suspend repository shape, and near-cache/write-behind behavior.
- Only add JaVers-specific code for mapping JaVers `CdoSnapshot` / commit metadata into those existing contracts, or for behavior that cannot be represented by the existing cache/exposed modules.
- Treat JDBC + virtual threads as a first-class performance mode. If the implementation introduces locks or blocking coordination, prefer explicit lock primitives and bounded queues over `synchronized` / `@Synchronized`, and validate the JDBC path under virtual-thread execution.

Kafka has two useful API surfaces for this repo. Spring Kafka `KafkaTemplate` wraps producer operations and returns `CompletableFuture` send results. The vanilla Kafka `KafkaProducer` is thread-safe, asynchronous, supports batching, idempotence, and transactions, and should support a Spring-free adapter. Because this project already depends on `bluetape4k-kafka` patterns elsewhere, a vanilla adapter should reuse those helpers where practical.

NATS JetStream and AWS SQS are viable alternate pipeline adapters, but they differ from Kafka. JetStream returns publish acknowledgements and supports sync/async publishing. SQS is a hosted queue that decouples distributed components and provides `sendMessage` / `sendMessageBatch`, but ordering, FIFO grouping, deduplication, visibility timeout, and DLQ behavior must be documented separately from Kafka semantics.

## Recommended issue split

1. Reuse existing #131 for the multi-layer repository: Exposed as primary durable read store, Redis as optional cache/read projection, Kafka as optional write stream. Require reuse of `bluetape4k-exposed` cache/exposed repositories before adding JaVers-only abstractions, and keep the JDBC path virtual-thread friendly.
2. Create a Redis latency strategy issue for cache-aside/read-through/write-through/write-behind plus Redisson near-cache evaluation. Require reuse of `bluetape4k-projects/cache` and `bluetape4k-exposed/exposed-jdbc-{lettuce,redisson}`; include JDBC + virtual-thread validation when measuring latency.
3. Create a Redis client parity issue because Lettuce and Redisson repositories already exist. Scope it to JaVers parity over existing implementations, not new cache infrastructure.
4. Create a pluggable message pipeline issue for Kafka, NATS JetStream, SQS, and future transports.
5. Create a vanilla Kafka / `bluetape4k-kafka` issue so users are not forced into Spring Kafka.

## Assets

No image assets were needed.
