# 이슈 #308 Ktor JDBC blocking 경계

## 배경

`examples-javers-ktor`의 Ktor CIO route는 `suspend` handler 안에서 동기
Exposed JDBC transaction과 JaVers repository를 직접 호출하고 있었다. `suspend`라는
표시만으로는 호출 스레드가 바뀌지 않으므로 동시 요청에서 request dispatcher가 JDBC
대기로 점유될 수 있었다.

## 결정 또는 발견

- `javersKtorModule()`에 호출자 소유 `Database?`와 `CoroutineDispatcher`를 주입할 수
  있게 했다.
- `database`를 생략하면 기존 H2 인메모리 예제를 유지하고, 전달된 database는 module이
  닫지 않는 호출자 소유 리소스로 취급한다.
- 요청 경로의 `handler.handle`, `repository.load`, `repository.loadHistory`는
  `withContext(blockingDispatcher)` 경계 안에서 실행한다.
- `CancellationException`은 별도로 삼키지 않고 다시 전파한다. 동기 Exposed transaction의
  자원 정리는 transaction 블록의 기존 finally 경계를 유지한다.
- R2DBC 전환이나 공용 repository API 재설계는 이슈 #308 범위에 넣지 않았다.

## 결과

기본 Ktor 예제는 H2와 `Dispatchers.IO`로 계속 실행되며, 테스트에서는 named executor를
주입해 모든 JDBC/JaVers route 호출이 지정한 blocking dispatcher에서 실행되는지를
확인한다. 별도의 PostgreSQL 테스트는 `bluetape4k-testcontainers`의
`PostgreSQLServer.Launcher.postgres`를 사용해 Ktor HTTP 요청부터 Exposed command state와
JaVers history까지 실제 JDBC/database 경계를 검증한다. DB가 있는 이슈에서
Testcontainers를 생략하면 H2 호환성만 확인하게 되므로 통합 검증을 별도 테스트로
유지한다.

## 검증

- RED: 새 boundary/PostgreSQL 테스트를 먼저 추가한 뒤 기존 API에 `blockingDispatcher`,
  `database`, helper가 없어 compile 단계에서 의도한 누락이 확인됐다.
- GREEN: `./gradlew :examples-javers-ktor:test --tests
  'io.bluetape4k.javers.examples.ktor.OrderApiBlockingBoundaryTest'
  --no-configuration-cache --no-build-cache --no-parallel --no-daemon --console=plain`
  — 2 tests passed.
- 회귀 확인: `withBlockingDispatcher`를 임시로 `block()`만 호출하도록 바꾼 mutant에서는
  boundary test가 dispatch count 0으로 실패했다. 테스트가 dispatcher 경계 제거를 실제로
  잡는 것을 확인한 뒤 구현을 원복했다.
- PostgreSQL: `./gradlew :examples-javers-ktor:test --tests
  'io.bluetape4k.javers.examples.ktor.OrderApiPostgreSqlIntegrationTest'
  --no-configuration-cache --no-build-cache --no-parallel --no-daemon --console=plain`
  — 1 Testcontainers-backed test passed.
- 모듈 전체: `./gradlew :examples-javers-ktor:test --no-configuration-cache
  --no-build-cache --no-parallel --no-daemon --console=plain` — 10 tests passed.
- `:examples-javers-ktor:check`와 `git diff --check`가 통과했고, Kotlin 최종
  checklist를 완료했다.

## 향후 지침

Ktor 예제에서 동기 JDBC 또는 동기 외부 client를 호출할 때는 `suspend` route에 직접
호출하지 말고 호출자 소유 dispatcher 경계를 명시한다. dispatcher 주입 테스트와 실제
backend Testcontainers 테스트를 함께 유지해 스레드 경계와 database/driver semantics를
서로 대체하지 않도록 한다.
