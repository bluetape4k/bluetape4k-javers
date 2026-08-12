# Issue #309: history limit은 query 단계로 push down한다

## 문제

`AggregateRepository.loadHistory(id)`는 JaVers의 기본 limit만 사용했고, Ktor와
Spring history endpoint는 snapshot 전체를 materialize한 뒤 `take(limit)`으로
응답만 줄였습니다. 요청 limit이 작아도 저장소 조회·객체 materialization 비용은
줄지 않아 history가 큰 aggregate에서 latency와 heap pressure가 불필요하게
증가했습니다.

## 결정

- `loadHistory(id, limit)`을 제공하고 기본값 100으로 기존 호출 호환성을 유지한다.
- caller가 전달한 limit은 `requirePositiveNumber("limit")`로 검증한다.
- `QueryBuilder.byInstanceId(...).limit(limit)`에 limit을 직접 전달한다.
- Ktor와 Spring은 request limit을 정규화한 뒤 repository query에 전달하고, 응답
  mapping에서는 사후 `take()`를 수행하지 않는다.
- JaVers가 제공하는 newest-first ordering과 history not-found semantics는
  변경하지 않는다.

## TDD와 검증

RED 단계에서 `AggregateRepositoryTest`에 query의 `limit: '2'` 전달과 non-positive
limit 거부를 추가했고, API overload가 없다는 compile failure를 확인했다. GREEN
단계에서 최소 구현 후 다음 검증을 통과했다.

- `:javers-ddd:test`: query push-down 단위 회귀 포함 4 tests
- `:examples-javers-ktor:test`: H2와 `PostgreSQLServer.Launcher.postgres` 기반
  Testcontainers 경로에서 `limit=1`이 최신 `PAID` snapshot 하나만 반환
- `:examples-javers-spring-boot4:test`: H2와 `PostgreSQLServer.Launcher.postgres`
  기반 Testcontainers 경로에서 같은 bounded-history contract 검증

PostgreSQL 검증은 H2 호환 모드만으로는 확인할 수 없는 Exposed JDBC schema,
snapshot persistence, JaVers query execution 경계를 실제 database engine에서
확인하기 위해 유지한다. Redis/Kafka는 이 변경의 영향 범위가 아니므로 실행하지
않는다.

## 재발 방지 규칙

페이지·limit·projection 요구가 있는 read API는 collection을 먼저 무제한으로
읽고 메모리에서 자르지 않는다. 저장소/ORM/JaVers query에 가능한 한 동일한
bounded contract를 전달하고, unit test로 query parameter를 capture하며,
`bluetape4k-testcontainers` 기반 실제 backend integration test로 ordering과
response semantics를 함께 검증한다.
