# examples-javers-exposed-ddd

[English](./README.md) | 한국어

JaVers, Exposed JDBC, Kafka, Redis, `javers-ddd` helper를 함께 사용하는 CQRS
예제입니다.

## 흐름

이 예제는 두 책임을 분리합니다. command side는 주문 row와 JaVers audit
snapshot을 소유합니다. event/read side는 주문 domain event를 Kafka로
발행하고, Redis `OrderSummary` projection으로 반영해 query read를 처리합니다.

![javers-exposed-ddd CQRS flow](../../docs/images/readme-diagrams/examples-javers-exposed-ddd-cqrs-flow-01.png)

Command handler는 Kafka event projection 전에 aggregate state를 먼저
저장합니다. Read API는 의도적으로 Redis projection만 읽으며, `OrdersTable`이나
JaVers snapshot을 조회하지 않습니다.

![javers-exposed-ddd command and projection sequence](../../docs/images/readme-diagrams/examples-javers-exposed-ddd-sequence-01.png)

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
주장이 아닙니다. 이제 일반 example test가 behavior 검증에 집중하도록 전용
benchmark module에서 실행합니다. milliseconds per operation 값은 낮을수록
좋습니다.

명령:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

환경과 원시 결과는
[`docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json`](../../docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json)에
저장되어 있습니다.

![JaVers Exposed DDD benchmark comparison](../../docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png)

| Lane | insert ms/op | update ms/op | audit-query ms/op | 비고 |
|---|---:|---:|---:|---|
| Hibernate Envers | 1.084 | 1.528 | 10.236 | Hibernate entity revision table 경로입니다. audit query는 audited entity revision을 읽습니다. |
| JaVers in-memory | 0.490 | 0.939 | 12.208 | persistence adapter 전의 JaVers core diff/query 경로에 가깝습니다. |
| JaVers + Exposed repository | 3.039 | 1.765 | 0.309 | 예제 source table을 제외한 snapshot repository persistence/query 경로입니다. |
| JaVers + Exposed DDD path | 2.095 | 3.002 | 0.313 | `OrdersTable`과 aggregate repository orchestration을 포함한 예제 end-to-end 경로입니다. |

이 fresh run에서는 이전 JaVers + Exposed audit-query outlier가 재현되지
않았습니다. 이 benchmark는 여전히 H2 기반 문서화용 benchmark입니다. 표는
예제의 비용 구조를 이해하기 위한 자료이며, release 전체 성능 보장은
아닙니다.

### Commit Metadata Index 평가

Commit metadata index benchmark는 JaVers Exposed SQL pushdown 경로를
benchmark 전용 `author`, `commit_date` 인덱스 조합과 비교합니다.
Testcontainers PostgreSQL 18-alpine과 HikariCP를 사용하고, 전용
`kotlinx-benchmark` 모듈 안에서 `bluetape4k-jdbc`,
`bluetape4k-exposed-jdbc`, `bluetape4k-exposed-jdbc-tests`를 재사용합니다.
이 benchmark는 production schema 기본값을 변경하지 않습니다.

연결된 snapshot은 `2026-08-14T05:43:21Z`에 JDK 25.0.4(GraalVM JDK 25,
macOS aarch64)에서 warmup 1회, 측정 1회로 다시 생성했습니다. 제한된 smoke
evidence이며 release 전체 성능 주장이 아닙니다.

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark \
  --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](../../docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](../../docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s |
|---|---:|---:|---:|
| Baseline | 461.8 | 862.9 | 1316.6 |
| Author index | 372.0 | 406.2 | 930.4 |
| `commit_date` index | 244.8 | 328.7 | 972.3 |
| Author + `commit_date` indexes | 342.9 | 543.0 | 1184.2 |

## 실행

```bash
./gradlew :examples-javers-exposed-ddd:test
```
