# JaVers Exposed Benchmark

[English](README.md) | 한국어

이 모듈은 JaVers Exposed persistence를 위한 제한된
`kotlinx-benchmark`/JMH smoke benchmark를 담습니다. benchmark 코드는 일반
example test와 분리되어 있어 CI와 full Nightly가 benchmark drift를 의도적으로
검증할 수 있습니다.

## 범위

`ExposedCommitMetadataIndexBenchmark`는 JaVers Exposed commit metadata table의
선택적 보조 인덱스를 측정합니다.

- `baseline`: 현재 production schema.
- `author`: benchmark 전용 `author` 인덱스.
- `commit_date`: benchmark 전용 `commit_date` 인덱스.
- `both`: benchmark 전용 `author` 및 `commit_date` 인덱스.

`EnversComparisonBenchmark`는 Exposed DDD example에서 사용하는 제한된 audit
workflow 경로를 비교합니다.

- `envers`: Hibernate Envers entity revision.
- `javers_in_memory`: persistence adapter 전 JaVers core diff/query 비용.
- `javers_exposed_repository`: JaVers Exposed snapshot repository 비용.
- `javers_exposed_ddd`: source table persistence와 aggregate repository orchestration.

benchmark는 trial마다 임시 PostgreSQL table을 만들고 tear-down에서 제거합니다.
이 모듈은 production JaVers Exposed schema 기본값을 변경하지 않습니다.

## 실행

CI와 full Nightly에서 사용하는 smoke run:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

Envers 비교 smoke run:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

로컬 full benchmark target:

```bash
./gradlew :benchmark-javers-exposed-benchmark:mainBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

benchmark task는 직렬로 실행해야 합니다. 이 모듈은 PostgreSQL Testcontainers,
HikariCP, `bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`,
`bluetape4k-exposed-jdbc-tests`를 사용합니다. 로컬 실행과 CI에서는 기본적으로
컨테이너를 재사용하지 않습니다. 개발자가 로컬에서만 재사용하려면
`~/.testcontainers.properties`에서 Testcontainers reuse를 활성화하고
`JAVA_TOOL_OPTIONS`에 `-Dbluetape4k.testcontainers.reuse=true`를 명시해야 합니다.
`CI` 또는 `GITHUB_ACTIONS` 환경 변수가 존재하면 값과 관계없이 이 opt-in은 무시됩니다. 재사용
PostgreSQL 컨테이너는 benchmark JVM 종료 후에도 실행 상태를 유지하며, 재사용하지
않는 컨테이너는 해당 benchmark JVM 종료 시 닫힙니다.

## 결과 Snapshot

커밋된 snapshot은 `2026-08-14T05:43:21Z`에 JDK 25.0.4(GraalVM JDK 25,
macOS aarch64)에서 warmup 1회, 측정 1회로 다시 생성했습니다. 점수는 초당
처리량이며 높을수록 좋습니다. 커밋한 JMH 각 row에는 `generatedAt`과
`sourceCommand` provenance 필드도 포함합니다. 실행 parameter는
`threads=1`, `forks=1`, warmup 1초, 측정 1초입니다.

Raw artifact:
[`docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`](../../docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json).

![JaVers Exposed commit metadata index evaluation](../../docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png)

| Variant | Insert ops/s | Author query ops/s | Date-range query ops/s | Decision signal |
|---|---:|---:|---:|---|
| Baseline | 461.8 | 862.9 | 1316.6 | 현재 production schema의 기준선이며 smoke evidence로만 봅니다. |
| Author index | 372.0 | 406.2 | 930.4 | JDK 25의 이번 제한된 실행에서는 모든 경로의 처리량이 낮았습니다. |
| `commit_date` index | 244.8 | 328.7 | 972.3 | 짧은 실행만으로 기본 인덱스를 권고하지 않습니다. |
| Author + `commit_date` indexes | 342.9 | 543.0 | 1184.2 | read throughput은 일부 회복되지만 insert throughput은 낮습니다. |

이 결과는 로컬 smoke evidence이며 release-wide 성능 주장이 아닙니다. production
schema를 바꾸려면 더 넓은 workload benchmark가 필요합니다.

Hosted receipt 계약은
[`docs/benchmark/benchmark-receipt-schema.md`](../../docs/benchmark/benchmark-receipt-schema.md)에
정의되어 있습니다. CI gate는 모든 scenario/variant row를 요구하고 teardown
failure receipt가 남으면 실패합니다.
