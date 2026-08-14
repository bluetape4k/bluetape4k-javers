# 이슈 #312 JDK 25 benchmark provenance와 Testcontainers 검증

## 배경

저장소 기본 build baseline은 Kotlin 2.4와 JDK 25이지만, JaVers Exposed
commit-metadata index snapshot은 JDK 21 결과를 가리키고 있었습니다. 이 상태로
표를 유지하면 JDK 25 실행과 직접 비교할 수 없는 결과를 현재 기준선처럼 읽을
위험이 있습니다.

## 결정

기존 benchmark module과 `bluetape4k-testcontainers` 기반 PostgreSQL harness를
재사용해 issue가 지정한 smoke command를 JDK 25에서 순차 실행했습니다. 후보
`author`/`commit_date` 인덱스는 benchmark 전용 table에만 적용하며 production
schema 기본값은 변경하지 않습니다.

커밋한 JMH artifact는 다음 provenance를 보존합니다.

- 생성 시각: `2026-08-14T05:43:21Z`
- runtime: JDK `25.0.4`, GraalVM JDK 25, macOS aarch64
- JMH: `1.37`, warmup 1회, 측정 1회, `threads=1`, `forks=1`
- 데이터베이스: PostgreSQL 18-alpine via Testcontainers, HikariCP
- raw artifact: `docs/benchmark/2026-06-08-javers-exposed-commit-metadata-indexes.json`

기존 README와 example 링크를 보존하기 위해 artifact 파일명은 유지하고,
실제 생성일은 각 JSON row의 `generatedAt`으로 구분합니다.

## 결과

| 변형 | 삽입 ops/s | 작성자 쿼리 ops/s | 날짜 범위 쿼리 ops/s |
|---|---:|---:|---:|
| 기준선 | 461.8 | 862.9 | 1316.6 |
| 작성자 인덱스 | 372.0 | 406.2 | 930.4 |
| `commit_date` 인덱스 | 244.8 | 328.7 | 972.3 |
| 작성자 + `commit_date` 인덱스 | 342.9 | 543.0 | 1184.2 |

이번 실행은 각 변형을 한 번 측정한 bounded smoke evidence입니다. 결과의 방향과
환경 provenance를 확인하는 데 사용하지만, release-wide capacity model이나
production DDL 변경의 단독 근거로 사용하지 않습니다.

## 재현 명령

```bash
./gradlew :benchmark-javers-exposed-benchmark:test :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark \
  --no-build-cache --no-daemon --no-parallel --console=plain
```

`benchmark/javers-exposed-benchmark`는 `bluetape4k.testcontainers`,
`bluetape4k-exposed-jdbc-tests`, PostgreSQL Testcontainers를 통해 실제 DB
통합 경로를 실행합니다. benchmark task와 다른 Testcontainers/Gradle 작업은
공유 상태를 피하기 위해 동시에 실행하지 않습니다.

## 향후 규칙

JDK 또는 benchmark parameter가 바뀌면 기존 숫자를 새 baseline으로 덮어쓰지
말고, JSON의 runtime·생성 시각·parameters와 EN/KO README 표를 같은 실행
artifact에서 함께 갱신합니다. JDK가 다른 기존 결과는 historical/non-comparable
evidence라고 명시하고, smoke 결과만으로 production index 기본값을 결정하지
않습니다.
