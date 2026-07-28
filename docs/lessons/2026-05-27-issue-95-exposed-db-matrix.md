# 이슈 95 Exposed DB 매트릭스

## 배경

`javers-exposed`에는 로컬 PostgreSQL/MySQL Testcontainers 연결을 사용하는
데이터베이스 스모크 테스트가 있었다. 워크스페이스에는 이미 공용
`bluetape4k-exposed-jdbc-tests` 매트릭스 도우미가 있다.

## 결정

중앙 `bt4k` 카탈로그를 통해 `bluetape4k-exposed-jdbc`와
`bluetape4k-exposed-jdbc-tests`를 재사용한다. 기본 매트릭스가 Exposed의 H2,
PostgreSQL, MySQL_V8과 일치하도록 `AbstractExposedTest`,
`@MethodSource(ENABLE_DIALECTS_METHOD)`, `withTables`로 테스트한다. 특정
다이얼렉트가 실제로 지원하지 않는 시나리오에만 JUnit `Assumptions`를 사용한다.

## 결과

이제 스모크 테스트는 공용 Exposed 매트릭스에서 실행된다. `saveSnapshot()`은
표준 H2에서 실패하던 Exposed `insertIgnore`에 더 이상 의존하지 않는다. 커밋
메타데이터는 다이얼렉트 중립적인 존재 여부 검사 후 삽입된다.

## 검증

- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryDatabaseSmokeTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  실행 결과 H2, PostgreSQL, MYSQL_V8 테스트 3개가 통과했다.
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  실행 결과 테스트 10개가 통과했다.

## 향후 지침

`javers-exposed` 데이터베이스 테스트 범위를 추가할 때는 임시 컨테이너보다
Exposed 공용 테스트 도우미를 우선한다. 필수 기준 시나리오는
H2/PostgreSQL/MySQL_V8에서 이식 가능하게 유지하고, 명시적으로 지원하지 않는
데이터베이스 기능에만 `Assumptions`를 사용한다.
