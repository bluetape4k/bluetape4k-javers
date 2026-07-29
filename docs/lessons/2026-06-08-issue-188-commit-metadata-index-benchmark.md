# 이슈 #188 커밋 메타데이터 인덱스 벤치마크

## 배경

이슈 #188에서는 SQL 푸시다운 쿼리를 위해 JaVers Exposed 커밋 메타데이터 테이블에
`author`와 `commit_date` 보조 인덱스를 추가해야 하는지 검토했다.

첫 시도에서는 JUnit 방식의 문서화 벤치마크를 사용했지만, 프로덕션 DDL을
결정하기에는 근거가 충분하지 않았다. 벤치마크를 `kotlinx-benchmark`와 JMH를
사용하는 전용 `benchmark/javers-exposed-benchmark` 모듈로 옮겼다.

## 결정

현재 프로덕션 JaVers Exposed 스키마는 변경하지 않는다. 후보 인덱스는
벤치마크 시험용 테이블 안에서만 생성한다.

스모크 실행에는 Testcontainers의 PostgreSQL 18-alpine, HikariCP,
`bluetape4k-jdbc`, `bluetape4k-exposed-jdbc`,
`bluetape4k-exposed-jdbc-tests`.

## 결과

가장 최근의 스모크 실행 결과는 다음과 같다.

| 변형 | 삽입 ops/s | 작성자 쿼리 ops/s | 날짜 범위 쿼리 ops/s |
|---|---:|---:|---:|
| 기준선 | 481.4 | 917.5 | 916.5 |
| 작성자 인덱스 | 488.6 | 907.1 | 904.7 |
| `commit_date` 인덱스 | 499.3 | 931.2 | 923.2 |
| 작성자 + `commit_date` 인덱스 | 518.6 | 945.9 | 873.8 |

이 결과만으로 기본 읽기 푸시다운 인덱스를 추가할 수는 없다. 인덱스를 적용한
변형이 일부 스모크 점수를 개선했지만, 복합 인덱스는 날짜 범위 처리량을
낮췄으며 프로덕션 DDL 정책을 정하기에는 실행 시간이 너무 짧았다.

루트 README에는 이전 Hibernate Envers / JaVers 인메모리 비교와 함께 해석한
결과도 필요하다. #188 처리량을 `1000 / opsPerSecond`로 연산당 대략적인
밀리초로 변환한 뒤, 이전 비교는 더 넓은 감사 워크플로를 측정하고 #188은 더
좁은 커밋 메타데이터 푸시다운 경로를 측정한다는 점을 설명한다. 두 벤치마크
계열을 동일한 워크로드로 제시해서는 안 된다. 통합 비교를 작성할 때는 삽입,
갱신, 읽기 측 결과를 함께 포함한다. 한 벤치마크 계열에 대응하는 시나리오가
없다면 해당 축을 빼지 말고 측정하지 않았다고 명시한다.

## 검증

- `./gradlew :benchmark-javers-exposed-benchmark:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :benchmark-javers-exposed-benchmark:mainCommitMetadataSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `xmllint --noout docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg`
- `rsvg-convert docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.svg -o docs/images/readme-charts/javers-exposed-commit-metadata-indexes-01.png`

## 향후 규칙

JaVers Exposed DDL 성능을 결정할 때는 PostgreSQL과 HikariCP 근거를 갖춘 전용
벤치마크 모듈을 사용한다. JUnit 문서화 벤치마크만을 프로덕션 인덱스 변경의
유일한 근거로 사용하지 않는다.
