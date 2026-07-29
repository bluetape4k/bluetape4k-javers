---
title: JaVers multilayer repository cache and pipeline research
date: 2026-06-04
source_type: official-docs-plus-repo-check
repo: bluetape4k/bluetape4k-javers
---

# JaVers multilayer repository cache 및 pipeline 연구

## Source link

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

## Repo-local 사실

- `bluetape4k-javers`에는 이미 `javers-exposed`, `javers-persistence-redis`, `javers-persistence-kafka`가 있다.
- `javers-persistence-redis`는 이미 `RedissonCdoSnapshotRepository`와 `LettuceCdoSnapshotRepository`를 모두 제공한다.
- `javers-persistence-kafka`는 현재 Spring Kafka `KafkaTemplate`을 사용하고 명시적으로 write-only다.
- `bluetape4k-projects/cache/cache-core`는 provider-neutral `NearCacheOperations`, `SuspendNearCacheOperations`, resilience decorator, Caffeine 같은 local cache provider, shared near-cache test guidance를 이미 제공한다.
- `bluetape4k-projects/cache/cache-lettuce`는 Lettuce native/JCache near-cache implementation, RESP3 client-tracking invalidation, resilient near-cache variant, benchmark evidence를 이미 제공한다.
- `bluetape4k-projects/cache/cache-redisson`은 Redisson JCache, `RedissonNearCache`, `RedissonSuspendNearCache`, `RLocalCachedMap`-backed near-cache support, memoizer를 이미 제공한다.
- `bluetape4k-exposed/exposed/exposed-cache`는 cache-backend-agnostic Exposed repository contract, `CacheMode`, `CacheWriteMode`, `LocalCacheConfig`, Redis-specific repository contract, reusable test fixture를 이미 정의한다.
- `bluetape4k-exposed/exposed/exposed-jdbc-lettuce`는 Exposed JDBC + Lettuce Redis read-through, write-through, write-behind, synchronous/suspended repository contract, Exposed-based `MapLoader` / `MapWriter`를 이미 구현한다.
- `bluetape4k-exposed/exposed/exposed-jdbc-redisson`은 Exposed JDBC + Redisson read-through, write-through, write-behind, near-cache, synchronous/suspended repository contract, Exposed-based `MapLoader` / `MapWriter`를 이미 구현한다.
- Java virtual thread에서 실행할 수 있으면 JDBC-backed Exposed repository가 선호되는 high-throughput baseline이다. Blocking JDBC path는 단순하게 유지되고 virtual thread는 request/thread contention을 줄인다. 따라서 JaVers JDBC cache code는 monitor-based synchronization을 피하고 virtual-thread friendliness를 보존해야 한다.
- Issue #89 / PR #92는 Exposed command-side snapshot, Kafka event, Redis projection으로 구성된 example CQRS flow를 이미 추가했다.
- Issue #105는 read-capable Kafka audit projection path용 open design issue다.
- Issue #131은 durable history plus event stream용 open composite repository issue다. 이 research는 이를 milestone `0.3.0`으로 갱신했다.

## 연구 종합

Redis cache-aside는 repeated read latency를 낮추는 conservative baseline이다. Redis를 먼저 읽고, database로 fall back하며, load한 value를 Redis에 write하고, primary write에서 invalidate한다. Redis 문서는 TTL-bounded staleness, explicit invalidation, partial update용 hash/JSON, Lua-based stampede mitigation을 유용한 구성 요소로 설명한다.

Redisson은 true repository-level cache layer에 더 강한 feature set을 제공한다. `RMap` family는 `MapLoader` 기반 read-through, synchronous `MapWriter` 기반 write-through, delayed batched `MapWriter` 기반 write-behind를 지원한다. `RLocalCachedMap`은 client side near-cache와 instance 간 invalidation/update synchronization을 추가한다. 이는 Redis + Exposed + DB latency layer와 직접 관련되지만, implementation은 staleness, replay/rebuild, failure semantics를 explicit하게 해야 한다.

Lettuce는 synchronous, asynchronous, reactive access용 좋은 low-level Redis client다. JaVers Redis repository에 이미 있으므로 새 작업은 duplicate repository를 만드는 대신 parity test, selection guidance, transactional connection misuse 방지에 집중해야 한다.

구현 planning에서 JaVers 작업은 fresh cache/exposed abstraction을 처음부터 만들면 안 된다. 필요한 path는 기존 bluetape4k cache 및 Exposed cache surface를 재사용하고 adapt하는 것이다.

- Provider-neutral near-cache behavior, statistics, resilience, test expectation에는 `bluetape4k-projects/cache/cache-core` contract를 사용한다.
- JaVers-specific near-cache wrapper를 추가하기 전에 `bluetape4k-projects/cache/cache-lettuce` 및 `cache/cache-redisson` provider implementation을 사용한다.
- Read-through, write-through, write-behind, `CacheMode`, `CacheWriteMode` semantics에는 `bluetape4k-exposed/exposed/exposed-cache` contract와 test fixture를 사용한다.
- Exposed + Redis + DB behavior의 baseline으로 `bluetape4k-exposed/exposed/exposed-jdbc-lettuce` 및 `exposed/exposed-jdbc-redisson` repository implementation을 사용한다. 특히 `MapLoader` / `MapWriter`, sync/suspend repository shape, near-cache/write-behind behavior를 기준으로 삼는다.
- JaVers-specific code는 JaVers `CdoSnapshot` / commit metadata를 기존 contract에 mapping하거나 기존 cache/exposed module로 표현할 수 없는 behavior에 대해서만 추가한다.
- JDBC + virtual thread를 first-class performance mode로 취급한다. 구현이 lock 또는 blocking coordination을 도입하면 `synchronized` / `@Synchronized`보다 explicit lock primitive와 bounded queue를 선호하고 virtual-thread execution에서 JDBC path를 validate한다.

Kafka에는 이 repo에 유용한 API surface가 두 개 있다. Spring Kafka `KafkaTemplate`은 producer operation을 wrap하고 `CompletableFuture` send result를 반환한다. Vanilla Kafka `KafkaProducer`는 thread-safe 및 asynchronous이고 batching, idempotence, transaction을 지원하므로 Spring-free adapter를 지원해야 한다. 이 project가 다른 곳에서 이미 `bluetape4k-kafka` pattern에 의존하므로 vanilla adapter는 practical한 범위에서 해당 helper를 재사용해야 한다.

NATS JetStream과 AWS SQS는 viable alternate pipeline adapter지만 Kafka와 다르다. JetStream은 publish acknowledgement를 반환하고 sync/async publishing을 지원한다. SQS는 distributed component를 decouple하고 `sendMessage` / `sendMessageBatch`를 제공하는 hosted queue지만 ordering, FIFO grouping, deduplication, visibility timeout, DLQ behavior는 Kafka semantics와 별도로 문서화해야 한다.

## 권장 issue split

1. Multi-layer repository에는 기존 #131을 재사용한다. Exposed는 primary durable read store, Redis는 optional cache/read projection, Kafka는 optional write stream으로 둔다. JaVers-only abstraction을 추가하기 전에 `bluetape4k-exposed` cache/exposed repository 재사용을 요구하고 JDBC path는 virtual-thread friendly하게 유지한다.
2. Cache-aside/read-through/write-through/write-behind plus Redisson near-cache evaluation용 Redis latency strategy issue를 만든다. `bluetape4k-projects/cache`와 `bluetape4k-exposed/exposed-jdbc-{lettuce,redisson}` 재사용을 요구하고 latency 측정 시 JDBC + virtual-thread validation을 포함한다.
3. Lettuce와 Redisson repository가 이미 있으므로 Redis client parity issue를 만든다. Scope는 새 cache infrastructure가 아니라 기존 implementation 위의 JaVers parity로 제한한다.
4. Kafka, NATS JetStream, SQS, future transport용 pluggable message pipeline issue를 만든다.
5. User가 Spring Kafka를 강제받지 않도록 vanilla Kafka / `bluetape4k-kafka` issue를 만든다.

## Assets

Image asset은 필요하지 않았다.
