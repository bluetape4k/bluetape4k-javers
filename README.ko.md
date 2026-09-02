# bluetape4k-javers

[![CI](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-25-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](./README.md) | 한국어

현재 안정 버전: `1.0.0`

현재 개발선: `develop`의 `1.1.0-SNAPSHOT`

![bluetape4k JaVers 감사 작업대 일러스트](./docs/assets/javers-workbench.png)

[JaVers](https://javers.org) 객체 감사(audit)와 diff를 위한 Kotlin/JVM 통합
라이브러리입니다. bluetape4k 애플리케이션이 SQL snapshot, Redis snapshot
state, Kafka audit event, DDD command-side 예제를 실제 요구에 맞게 고를 수
있도록 JaVers wiring을 정리합니다.

## 프로젝트 목적

`bluetape4k-javers`는 JaVers의 object diff 모델을 쓰되, bluetape4k 방식의
Kotlin API, 명시적인 persistence 선택지, 실행 가능한 예제가 필요한 서비스를
위한 저장소입니다. 먼저 `javers-core`로 시작하고, 애플리케이션의 audit 계약에
맞는 persistence adapter를 더한 뒤, 예제와 benchmark로 운영 tradeoff를
확인하는 흐름을 의도합니다.

가장 중요한 결정은 audit snapshot의 authoritative store입니다. Exposed는 SQL
query 경로, Redis는 low-latency snapshot state 경로, Kafka는 projection을 위한
event delivery 경로입니다. 함께 쓸 수는 있지만 서로 대체 가능한 저장소로 보면
안 됩니다.

## 제공 기능

- **Core JaVers helper** — Kotlin extension, codec, cache delegate, composite CDO snapshot repository
- **Exposed JDBC persistence** — SQL-backed snapshot, repository head 복원, Envers 경로와 비교 가능한 query behavior
- **DDD helper** — JaVers commit 주변의 aggregate repository, domain event, publisher boundary
- **Spring Boot 4 auto-configuration** — Exposed, Redis, Kafka backend를 위한 조건부 JaVers repository와 builder wiring
- **Redis persistence** — repository rebuild 이후에도 복원 가능한 low-latency snapshot state를 위한 Lettuce/Redisson 경로
- **Kafka persistence** — 직접 history query가 아니라 snapshot event delivery와 projection pipeline을 위한 경로
- **실행 가능한 예제** — Exposed DDD CQRS, Ktor REST, Spring Boot 4 REST wiring
- **BOM 지원** — 소비자 dependency version 정렬을 위한 `bluetape4k-javers-bom`

## Persistence 선택지

![JaVers persistence options relationship diagram](docs/images/readme-diagrams/javers-persistence-options-01.png)

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
| `javers-persistence-redis` | `io.github.bluetape4k.javers:javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `javers-persistence-kafka` | `io.github.bluetape4k.javers:javers-persistence-kafka` | projection을 위한 Kafka-backed CDO snapshot event stream |
| `examples-javers-exposed-ddd` | example module | Exposed persistence와 JaVers DDD helper를 사용하는 CQRS command-side 예제 |
| `examples-javers-ktor` | example module | 명시적 Exposed/JaVers wiring을 사용하는 Ktor REST 예제 |
| `examples-javers-spring-boot4` | example module | 명시적 Exposed/JaVers wiring을 사용하는 Spring Boot 4 REST 예제 |
| `benchmark-javers-exposed-benchmark` | 배포하지 않는 benchmark module | JaVers Exposed commit-metadata index evidence를 위한 제한된 JMH/Testcontainers benchmark |
| `javers-spring-boot4-autoconfigure` | `io.github.bluetape4k.javers:javers-spring-boot4-autoconfigure` | Exposed, Redis, Kafka JaVers repository를 위한 Spring Boot 4 조건부 auto-configuration |
| `bluetape4k-javers-bom` | `io.github.bluetape4k.javers:bluetape4k-javers-bom` | JaVers artifact 정렬용 consumer BOM |

## 매뉴얼

- [현재 1.1.0 개발 매뉴얼](https://github.com/bluetape4k/bluetape4k.github.io/blob/develop/docs/manual/bluetape4k-javers/current/ko/index.md)은
  `settings.gradle.kts`에 등록된 모든 모듈과 예제·benchmark 근거를 다룹니다.
- [릴리스 고정 1.0.0 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-javers/1.0/)은 immutable 상태로
  보존하며 해당 릴리스에서 제공한 모듈만 설명합니다.

## bluetape4k-exposed와의 경계

`bluetape4k-javers`는 JaVers audit와 history 의미를 담당합니다. 객체 diff, CDO
snapshot, commit metadata, shadow query, JaVers를 의식한 aggregate workflow가 필요할
때 사용하는 저장소입니다. 애플리케이션 source-of-truth Repository나 cache runtime을
소유하지는 않습니다.

| 영역 | 책임 | 책임이 아닌 것 |
|---|---|---|
| `bluetape4k-exposed` | Exposed Repository 실행, 트랜잭션 경계, cache read/write 동작, Spring Boot/Ktor Exposed adapter | JaVers audit history, CDO snapshot 저장, JaVers commit metadata |
| `javers-exposed` | Exposed JDBC 기반 JaVers CDO snapshot과 commit persistence | 애플리케이션 Repository, write-through/write-behind cache mode, Ktor/Spring Exposed runtime helper |
| `javers-ddd` | JaVers를 의식한 aggregate save flow, domain event 기반 JaVers commit property, JaVers commit 주변 event publisher adapter | 모든 Exposed 애플리케이션을 위한 범용 DDD base model |

두 저장소를 함께 사용할 때는 Exposed Repository를 애플리케이션 상태의 source of
truth로 두고, JaVers가 그 상태에서 audit history를 기록하게 합니다. Cache-aside,
read-through, write-through, write-behind, near-cache 동작은 전용 composite JaVers
repository가 replay, invalidation, failure semantics를 명시적으로 소유하지 않는 한
애플리케이션 read model이나 projection에 머물러야 합니다.

## 의존성 설정

여러 모듈을 함께 사용할 때는 BOM을 사용하세요:

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:1.0.0"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
    implementation("io.github.bluetape4k.javers:javers-ddd")
    implementation("io.github.bluetape4k.javers:javers-spring-boot4-autoconfigure")
}
```

Exposed, Redis, Kafka 모듈은 역할이 다르므로 애플리케이션이 실제로 사용하는
storage/eventing adapter만 추가하세요. BOM은 버전을 맞춰주지만 runtime topology를
대신 결정하지는 않습니다.

## 빠른 시작

```kotlin
val snapshotRepository = ExposedCdoSnapshotRepository(database)
snapshotRepository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(snapshotRepository)
    .registerEntity(Order::class.java)
    .build()
```

Spring Boot 4 애플리케이션은 auto-configuration 모듈을 추가하고 repository backend를
선택할 수 있습니다. Exposed `Database`, Redis client, Kafka producer,
`KafkaTemplate` 같은 인프라 bean은 여전히 애플리케이션이 소유합니다.

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-spring-boot4-autoconfigure")
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

```yaml
bluetape4k:
  javers:
    repository:
      type: exposed
```

DDD command flow에서는 source-of-truth 저장소에 aggregate를 저장하고 JaVers에
commit한 뒤 domain event를 발행합니다. `examples/javers-exposed-ddd` 모듈은 이
경로를 Kafka event와 Redis read model까지 포함해 보여줍니다.

`examples/javers-spring-boot4` 모듈은 같은 JaVers + Exposed command persistence
흐름을 Spring Boot 4 REST endpoint 뒤에서 명시적으로 wiring해 보여줍니다. 수동
wiring을 선호할 때 참고할 수 있습니다.

`examples/javers-ktor` 모듈은 같은 현재 기능을 non-Spring 사용자를 위한 Ktor REST
endpoint 뒤에서 보여주며, bluetape4k Ktor JSON/health helper를 재사용합니다.

## 요구사항

- JDK 25+
- Kotlin 2.4+
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
./gradlew :javers-spring-boot4-autoconfigure:test
```

## 벤치마크 스냅샷

아래 비교는 문서용으로 범위를 제한한 benchmark이며 release-wide 성능 주장이
아닙니다. 아래 Envers snapshot은 JDK 21에서 측정한 historical evidence이므로
신선한 JDK 25 commit-metadata 실행과 직접 비교하지 않습니다.
새 실행은 전용 benchmark module 명령
`./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
을 사용합니다. 아래 committed snapshot은 scenario마다 warmup 5회와 측정 40회를
실행해 생성했습니다. 단위는 milliseconds per operation이며 낮을수록 좋습니다.

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

커밋된 snapshot은 `2026-08-14T05:43:21Z`에 JDK 25.0.4(GraalVM JDK 25,
macOS aarch64)에서 warmup 1회, 측정 1회로 다시 생성했습니다. JSON row에는
`generatedAt`과 `sourceCommand` provenance 필드가 남아 있습니다. 실행
parameter는 `threads=1`, `forks=1`, warmup 1초, 측정 1초입니다.

명령:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s | Decision signal |
|---|---:|---:|---:|---|
| Baseline | 461.8 | 862.9 | 1316.6 | 현재 production schema의 기준선이며 smoke evidence로만 봅니다. |
| Author index | 372.0 | 406.2 | 930.4 | JDK 25의 이번 제한된 실행에서는 모든 경로의 처리량이 낮았습니다. |
| `commit_date` index | 244.8 | 328.7 | 972.3 | 짧은 실행만으로 기본 인덱스를 권고하지 않습니다. |
| Author + `commit_date` indexes | 342.9 | 543.0 | 1184.2 | read throughput은 일부 회복되지만 insert throughput은 낮습니다. |

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
