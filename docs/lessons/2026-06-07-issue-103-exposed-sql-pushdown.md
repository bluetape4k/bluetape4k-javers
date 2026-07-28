# Issue #103 Exposed SQL 푸시다운

## 배경

이전의 `javers-exposed`는 광범위한 JaVers 스냅샷 쿼리에 공용
`AbstractCdoSnapshotRepository` 쿼리 경로를 사용했다. 이 경로는 공통 쿼리
필터를 적용하기 전에 저장소의 모든 키를 불러오고 스냅샷을 디코딩한다.

## 결정

영속 Exposed 컬럼으로 뒷받침되는 조건식만 푸시다운한다.

- 비 aggregate 쿼리의 global id 이력
- 비 aggregate 클래스 쿼리의 managed type 이력
- `getSnapshots(QueryParams)`의 commit id, 정확한 version, 정확한 author,
  `LocalDateTime` commit date 범위, snapshot type, `skip`, `limit`

JaVers 인메모리 의미론이 필요한 조건식은 기존 fallback 경로에 유지한다.

## 결과

이제 저장소는 public API 계약이나 저장된 스냅샷 payload 형식을 변경하지
않으면서 일반적인 JQL 경로에서 디코딩하는 스냅샷 행의 수를 줄인다.

## 검증

- `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain`
- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- DB matrix에서 H2, PostgreSQL, MySQL을 대상으로 테스트 36개를 실행했다.
- 전체 `:javers-exposed:test`에서 테스트 46개를 실행했다.

## 향후 보호 규칙

SQL 푸시다운을 추가할 때는 먼저 조건식이 명시적으로 영속화된 컬럼이나
dialect-safe 표현식으로 뒷받침되는지 입증한다. 그렇지 않다면 공용 저장소의
fallback 경로를 유지하고, 지원하지 않는 조건식을 README에 문서화한다.
