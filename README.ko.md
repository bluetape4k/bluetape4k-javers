# bluetape4k-javers

[![CI](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](./README.md) | 한국어

![bluetape4k JaVers 감사 작업대 일러스트](./docs/assets/javers-workbench.png)

[JaVers](https://javers.org) 객체 감사(audit)와 diff를 위한 Kotlin/JVM 통합 라이브러리입니다.
CDO snapshot과 event-sourced change stream을 위해 Exposed JDBC, Redis, Kafka persistence 선택지를 제공합니다.

## 프로젝트 목적

`bluetape4k-javers`는 JaVers의 기본 in-memory, MongoDB, JDBC 저장소 선택지를 확장합니다.
cache-backed read, Redis persistence, Kafka event stream, Exposed 기반 repository layer가
필요한 Kotlin 서비스의 audit/diff 인프라를 목표로 합니다.

JaVers의 object diff 모델을 쓰면서도 Kotlin-first helper, Exposed JDBC
persistence, cache/stream adapter, CQRS 예제를 함께 가져가야 하는 bluetape4k
서비스에 맞춘 저장소입니다.

## 제공 기능

- **JaVers core helper** — extension, codec, cache-backed repository 구성 요소
- **Exposed JDBC persistence** — SQL-backed JaVers CDO snapshot을 위한 Exposed schema와 repository
- **DDD helper** — JaVers audit workflow를 위한 aggregate root, domain event, repository, publisher adapter
- **CQRS command-side 예제** — `examples/javers-exposed-ddd` 아래 Exposed + JaVers + DDD helper 주문 command flow
- **Ktor REST 예제** — `examples/javers-ktor` 아래 Exposed command persistence와 JaVers audit history를 명시적으로 wiring하는 예제
- **Spring Boot 4 REST 예제** — `examples/javers-spring-boot4` 아래 Exposed command persistence와 JaVers audit history를 명시적으로 wiring하는 예제
- **Redis persistence** — Lettuce/Redisson 기반 snapshot 저장 경로
- **Kafka persistence** — event stream 기반 CDO snapshot persistence
- **BOM 지원** — 소비자 dependency version 정렬을 위한 `bluetape4k-javers-bom`
- **구현 backlog** — Exposed persistence, DDD helper, CQRS/Event Sourcing 예제 phase chain

## Persistence 선택지

![JaVers persistence options relationship diagram](./docs/assets/javers-persistence-options.png)

## 아키텍처

![javers Architecture diagram](docs/images/readme-diagrams/bluetape4k-javers-architecture-01.png)

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Bluetape4k JaVers overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Bluetape4k JaVers module composition chart](docs/images/readme-charts/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## 모듈

| 모듈 | Artifact | 역할 |
|---|---|---|
| `javers-core` | `io.github.bluetape4k.javers:javers-core` | JaVers extension, codec, cache-backed/composite repository |
| `javers-ddd` | `io.github.bluetape4k.javers:javers-ddd` | JaVers audit workflow용 DDD aggregate/domain-event helper |
| `javers-exposed` | `io.github.bluetape4k.javers:javers-exposed` | Exposed JDBC CDO snapshot persistence |
| `examples-javers-exposed-ddd` | example module | Exposed persistence와 JaVers DDD helper를 사용하는 CQRS command-side 예제 |
| `examples-javers-ktor` | example module | 명시적 Exposed/JaVers wiring을 사용하는 Ktor REST 예제 |
| `examples-javers-spring-boot4` | example module | 명시적 Exposed/JaVers wiring을 사용하는 Spring Boot 4 REST 예제 |
| `javers-persistence-redis` | `io.github.bluetape4k.javers:javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `javers-persistence-kafka` | `io.github.bluetape4k.javers:javers-persistence-kafka` | Kafka-backed CDO snapshot persistence (쓰기 전용 이벤트 스트림; 읽기는 항상 빈 결과 반환) |
| `bluetape4k-javers-bom` | `io.github.bluetape4k.javers:bluetape4k-javers-bom` | JaVers artifact 정렬용 consumer BOM |

## 의존성 설정

여러 모듈을 함께 사용할 때는 BOM을 사용하세요:

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:0.2.1"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
    implementation("io.github.bluetape4k.javers:javers-ddd")
}
```

Kafka, Redis, Exposed 모듈은 역할이 다르므로 애플리케이션이 실제로 사용하는
storage/eventing adapter만 추가하세요.

## 빠른 시작

```kotlin
val snapshotRepository = ExposedCdoSnapshotRepository(database)
snapshotRepository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(snapshotRepository)
    .registerEntity(Order::class.java)
    .build()
```

DDD command flow에서는 source-of-truth 저장소에 aggregate를 저장하고 JaVers에
commit한 뒤 domain event를 발행합니다. `examples/javers-exposed-ddd` 모듈은 이
경로를 Kafka event와 Redis read model까지 포함해 보여줍니다.

`examples/javers-spring-boot4` 모듈은 같은 JaVers + Exposed command persistence
흐름을 Spring Boot 4 REST endpoint 뒤에서 명시적으로 wiring해 보여줍니다. 아직
제공하지 않는 auto-configuration에 의존하지 않습니다.

`examples/javers-ktor` 모듈은 같은 현재 기능을 non-Spring 사용자를 위한 Ktor REST
endpoint 뒤에서 보여주며, bluetape4k Ktor JSON/health helper를 재사용합니다.

## 요구사항

- JDK 21+
- Kotlin 2.3+
- JaVers 7.11.0

## 빌드

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-ddd:test
./gradlew :javers-exposed:test
./gradlew :examples-javers-exposed-ddd:test
./gradlew :examples-javers-ktor:test
./gradlew :examples-javers-spring-boot4:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## 벤치마크 스냅샷

아래 비교는 문서용으로 범위를 제한한 benchmark이며 release-wide 성능 주장이
아닙니다.
`./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
명령으로 생성했고, scenario마다 warmup 5회와 측정 40회를 실행했습니다. 단위는
milliseconds per operation이며 낮을수록 좋습니다.

환경: PostgreSQL 18-alpine via Testcontainers, HikariCP, JDK 21.0.11, macOS
aarch64. Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json`](docs/benchmark/2026-06-08-javers-exposed-ddd-envers-comparison.json).

![JaVers Exposed DDD benchmark comparison](docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.png)

| Implementation | Insert ms/op | Update ms/op | Audit-query ms/op |
|---|---:|---:|---:|
| Hibernate Envers | 4.486 | 6.917 | 12.483 |
| JaVers in-memory | 0.510 | 0.978 | 12.559 |
| JaVers + Exposed repository | 8.499 | 5.945 | 0.763 |
| JaVers + Exposed DDD path | 6.397 | 7.257 | 0.704 |

Exposed lane은 HikariCP를 통한 PostgreSQL round trip을 포함합니다. DDD path는
source-of-truth order persistence와 aggregate repository orchestration까지
포함하므로, 순수 snapshot repository 비용이 아니라 end-to-end example path로
비교해야 합니다.

### Commit Metadata Index 평가

Issue #188은 JaVers Exposed commit metadata table에 `author`, `commit_date`
보조 인덱스를 추가할 가치가 있는지 평가합니다. 아래 benchmark는 전용
`kotlinx-benchmark`/JMH harness이며 Testcontainers의 PostgreSQL 18-alpine,
HikariCP, `bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`,
`bluetape4k-exposed-jdbc-tests`를 사용합니다. 점수 단위는 초당 처리량
operations per second이며, 높을수록 좋습니다.

명령:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s | Decision signal |
|---|---:|---:|---:|---|
| Baseline | 481.4 | 917.5 | 916.5 | 현재 production schema의 안정 기준선입니다. |
| Author index | 488.6 | 907.1 | 904.7 | 이 smoke run에서는 author query 이점이 없습니다. |
| `commit_date` index | 499.3 | 931.2 | 923.2 | read throughput이 약간 높지만 bounded evidence입니다. |
| Author + `commit_date` indexes | 518.6 | 945.9 | 873.8 | author query는 가장 높지만 date-range throughput은 낮습니다. |

후보 인덱스는 benchmark schema 안에서만 생성합니다. 더 넓은 workload
benchmark가 DDL 및 write-amplification 비용을 정당화하기 전까지 production
JaVers Exposed 기본 schema는 변경하지 않습니다. 이번 짧은 smoke run은
결과가 mixed입니다.

### 종합 해석

두 benchmark 계열은 서로 다른 질문에 답합니다. Envers 비교는 더 넓은 감사
workflow를 milliseconds per operation 단위로 측정하고, commit metadata
benchmark는 더 좁은 JaVers Exposed SQL pushdown 경로를 throughput으로
측정합니다. 아래 표는 metadata benchmark를 `1000 / opsPerSecond`로
milliseconds per operation 근사값으로 환산해 insert, update, read-side
결과를 같은 latency 눈금에서 읽을 수 있게 한 것입니다. commit metadata
benchmark는 이 repository 경로에서 append-only인 JaVers commit metadata를
다루므로 update scenario를 포함하지 않습니다.

![JaVers Exposed combined benchmark overview](docs/images/readme-charts/javers-exposed-combined-benchmark-overview-01.png)

| Path | Scope | Insert ms/op | Update ms/op | Read ms/op | Interpretation |
|---|---|---:|---:|---:|---|
| Hibernate Envers | Entity revision audit path | 4.486 | 6.917 | 12.483 audit-query | 예제 domain의 JPA audit 기준 경로입니다. |
| JaVers in-memory | Core JaVers diff/query path | 0.510 | 0.978 | 12.559 audit-query | write/update는 빠르지만 in-memory audit query는 Exposed SQL 경로가 아닙니다. |
| JaVers + Exposed repository | Snapshot repository path | 8.499 | 5.945 | 0.763 audit-query | write 비용은 높지만, 이 비교에서는 repository read path가 가장 빠른 full audit-query lane입니다. |
| JaVers + Exposed DDD path | End-to-end example path | 6.397 | 7.257 | 0.704 audit-query | source table과 aggregate orchestration을 포함하면서도 audit read가 빠릅니다. |
| JaVers Exposed metadata baseline | Commit metadata author/date filters | 2.077 | Not measured | 1.090 author / 1.091 date-range | metadata read 기준으로는 현재 production schema도 Exposed audit-query와 같은 order of magnitude 안에 있습니다. |
| Best metadata-index smoke variants | Benchmark-only candidate indexes | 1.928 | Not measured | 1.057 author / 1.083 date-range | smoke run의 insert는 약간 빠르지만 read 이득은 작고 mixed입니다. |

종합하면, 이 workload에서는 Envers 대비 Exposed repository 방향이 read-side에
유리하지만 insert/update 비용은 in-memory path보다 큽니다. commit metadata
index는 별도 결정으로 보수적으로 유지해야 합니다. 이번 smoke 결과는 insert와
일부 read filter에서만 작은 이득을 보였고, update는 append-only benchmark
범위 밖입니다.

## 참고 자료

- [JaVers](https://javers.org)
- [JaVers Feature Overview](https://javers.org/features)
- [JaVers VS Envers Comparison](https://javers.org/blog/2017/12/javers-vs-envers-comparision.html)
- [Using JaVers for Data Model Auditing in Spring Data](https://www.baeldung.com/spring-data-javers-audit)
- [Spring Data에서 데이터 모델 감사를 위해 JaVers 사용](https://recordsoflife.tistory.com/486)
