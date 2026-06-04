# examples-javers-exposed-ddd

[English](./README.md) | 한국어

JaVers, Exposed JDBC, Kafka, Redis, `javers-ddd` helper를 함께 사용하는 CQRS
예제입니다.

## 흐름

![javers-exposed-ddd command and projection sequence](docs/images/readme-diagrams/javers-exposed-ddd-sequence-01.png)

![javers-exposed-ddd CQRS flow](docs/images/readme-diagrams/javers-exposed-ddd-cqrs-flow-01.png)

## 포함 범위

- `AggregateRoot<OrderId>`를 구현하는 `Order` aggregate
- `OrderPlaced`, `OrderMarkedPaid` domain event
- Exposed 기반 source-of-truth 주문 저장
- `ExposedCdoSnapshotRepository`를 통한 JaVers snapshot 저장
- Kafka 기반 domain event 발행
- Redis 기반 `OrderSummary` projection
- `OrderQueryService` read-side 조회 API
- H2 command handler 테스트와 Kafka/Redis Testcontainers projection flow

## Benchmark

Envers 비교 benchmark는 로컬 문서화용 benchmark이며, release 전체 성능
주장이 아닙니다. H2에서 이 예제의 단순 persistence shape만 측정합니다.
milliseconds per operation 값은 낮을수록 좋습니다.

명령:

```bash
./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

환경과 원시 결과는
[`docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json`](../../docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json)에
저장되어 있습니다.

| Scenario | Hibernate Envers ms/op | JaVers + Exposed ms/op |
|---|---:|---:|
| insert | 1.049 | 3.627 |
| update | 1.383 | 2.848 |
| audit-query | 8.010 | 105.339 |

이번 H2 실행에서는 좁은 persistence benchmark 기준 Envers가 더 빠릅니다.
JaVers + Exposed 예제의 가치는 단순 entity revision table만이 아니라 명시적
aggregate commit, commit metadata, domain event integration, CQRS projection
경로가 필요할 때에 있습니다.

## 실행

```bash
./gradlew :examples-javers-exposed-ddd:test
```
