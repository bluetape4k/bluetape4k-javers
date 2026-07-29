# Issue #132 - JaVers Exposed 벤치마크 비용 분석

## 배경

Issue #90에서는 로컬 Envers 비교 벤치마크를 추가했지만, 문서에 기록된 JaVers +
Exposed 감사 쿼리 결과에는 큰 이상치가 있었다. Issue #132의 목표는 JaVers 핵심
비용과 Exposed 영속성/쿼리 비용을 분리하고 표와 차트로 결과를 설명하는 것이었다.
후속 리뷰에서는 벤치마크를 HikariCP를 사용하는 PostgreSQL에서 실행해야 하며
JaVers Exposed 테이블에는 대리 `LongIdTable` ID가 필요하지 않다는 점도 확인했다.

## 결정

이를 프로덕션 최적화 근거가 아닌 문서화용 벤치마크로 한정한다. PostgreSQL
Testcontainers + HikariCP 환경에서 다음 네 경로를 사용한다.

- `Hibernate Envers`
- `JaVers in-memory`
- `JaVers + Exposed repository`
- `JaVers + Exposed DDD path`

저장소 전용 경로는 스냅샷 저장소 동작만 분리한다. DDD 경로는 실제 예제 경로로
유지하며 `OrdersTable`, aggregate 저장소 오케스트레이션, 이벤트를 고려한 저장
동작을 포함한다.

Exposed JaVers 테이블에는 다음 자연 기본 키를 사용한다.

- `javers_commit`: `commit_id`
- `javers_snapshot`: `(global_id, version)`

이렇게 하면 대리 ID 기본 키와 중복된 고유 `(global_id, version)` 인덱스를 제거할
수 있다. `(global_id, commit_id)`를 스냅샷 키로 사용해서는 안 된다. JaVers
스냅샷의 정체성은 객체 ID와 객체 버전의 조합인 반면, 하나의 커밋 ID에는 여러
스냅샷이 묶일 수 있기 때문이다.

## 결과

PostgreSQL + HikariCP 실행에서는 기존 JaVers + Exposed 감사 쿼리 이상치가 더 이상
재현되지 않았다. 영문/한국어 README에는 원시 JSON 링크, PNG 차트, 결과 표, 실행
환경과 이 벤치마크가 문서화 용도로 한정된다는 주의 사항을 추가했다.

## 검증

- `./gradlew :examples-javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 테스트 1개, PostgreSQL + HikariCP JSON 생성.
- `./gradlew :javers-exposed:compileKotlin :examples-javers-exposed-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 테스트 4개, 최종 PostgreSQL + HikariCP JSON 생성.
- `./gradlew :javers-exposed:test --tests '*snapshot and commit tables declare natural keys and hot path indexes*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, H2/PostgreSQL/MySQL 매트릭스.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 테스트 53개.
- `xmllint --noout docs/images/readme-charts/javers-exposed-ddd-envers-comparison-01.svg`
  - PASS.
- `rsvg-convert`로 PNG 차트를 생성하고 육안으로 확인했다.
- `git diff --check`
  - PASS.

## 향후 지침

페이로드 형태가 명확하지 않다면 감사 벤치마크 결과 행을 서로 비교하지 않는다.
README에서 성능 방향을 설명할 때는 지표의 방향, 실행 환경, 원시 산출물 링크,
한정된 범위에 대한 주의 사항을 함께 제공한다. 지속적으로 인용할 성능 근거가
필요하다면 이 JUnit 문서화 벤치마크 대신 전용 JMH 벤치마크로 옮긴다. 향후 PR에서
`author` 또는 `commit_date` SQL 푸시다운 인덱스가 필요하다면 삽입 비용 측정과
함께 별도로 평가한다. 이 인덱스들은 커밋 테이블에 위치해 쓰기 오버헤드를
늘리기 때문이다. 후속 작업: #188.
