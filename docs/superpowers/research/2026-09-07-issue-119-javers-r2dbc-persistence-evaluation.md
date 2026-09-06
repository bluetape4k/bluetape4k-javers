# Issue #119: JaVers snapshot R2DBC persistence 평가

- 조사일: 2026-09-07
- 대상: `bluetape4k-javers`, JaVers `7.11.8` 기준
- 결론: **DEFER** — 현재 `JaversRepository` 계약 뒤에 R2DBC 구현을 추가하지 않는다.
- 신뢰도: 높음

## 결론 요약

현재 JaVers persistence SPI는 동기식이다. `JaversRepository`의 조회는
`List`/`Optional`, 저장과 schema 준비는 `void` 메서드이며 `suspend`,
`Publisher`, `CompletionStage` 경계가 없다. 따라서 기존 SPI를 그대로
구현하면서 Exposed R2DBC 또는 Spring Data R2DBC를 붙이면 저장소 내부의
일부 I/O만 비동기일 뿐, JaVers 호출 계약과 전체 snapshot 조회는
non-blocking이 되지 않는다.

이 이슈에서는 구현을 추가하지 않고, 현재의 Exposed JDBC adapter를
유지한다. 향후 실제 async 수요가 확인되면 먼저 JaVers 호출을 감싸는
별도 async facade/API와 transaction 전파 계약을 설계한 뒤, adapter와
테스트를 별도 이슈로 나눈다. Ktor/WebFlux에서 당장 event-loop를 보호해야
하는 경우에는 기존 JDBC 호출을 명시적인 blocking dispatcher로 격리하는
운영 완화책을 사용한다. 이것은 R2DBC 지원이 아니다.

## 조사 범위와 판정 기준

Issue #119의 acceptance에 맞춰 다음 후보를 비교했다.

1. 현재 `JaversRepository`를 직접 R2DBC로 구현
2. Exposed R2DBC 기반의 JaVers adapter
3. Spring Data R2DBC 기반의 JaVers adapter
4. 기존 JDBC adapter를 호출자 소유의 blocking dispatcher에서 실행

판정 기준은 (a) JaVers SPI가 요구하는 동기/비동기 경계, (b) transaction
소유권과 전파, (c) snapshot/head/sequence/query 의미 보존, (d) 테스트와
운영 범위이다.

## 근거

### 로컬 구현

| 근거 | 관찰 | 영향 |
| --- | --- | --- |
| `javers-core/.../CdoSnapshotRepository.kt:23-49` | `saveSnapshot`과 `loadSnapshots`가 일반 Kotlin 함수이며 `CdoSnapshotRepository`를 상속한다. | 현재 확장 계약에 async 반환형이 없다. |
| `javers-core/.../AbstractCdoSnapshotRepository.kt:55-69,119-129` | 저장소가 `ReentrantLock`을 사용하고 `getAll()`에서 모든 snapshot을 메모리에 적재한 뒤 정렬한다. | suspend 함수만 추가해도 동기 lock과 전체 materialization이 사라지지 않는다. |
| `javers-exposed/.../ExposedCdoSnapshotRepository.kt:104-123,125-237,382-384` | `org.jetbrains.exposed.v1.jdbc.Database`를 사용하고 모든 schema/read/write를 JDBC `transaction`으로 감싼다. | 현재 adapter는 명시적으로 JDBC 경계이다. |
| `javers-exposed/build.gradle.kts:13-37` | `bluetape4k-exposed-jdbc`, Exposed JDBC/DAO/java-time만 선언하며 R2DBC 의존성이 없다. | 이번 변경에 의존성만 추가해도 SPI 문제는 해결되지 않는다. |
| `docs/superpowers/specs/2026-05-26-issue-3-javers-exposed-design.md` | R2DBC repository를 현재 설계의 non-scope로 기록한다. | 기존 설계와도 일관되게 별도 설계가 필요하다. |

### 공식 문서와 소스

- [JaVers 7.11.8 `JaversRepository` source](https://raw.githubusercontent.com/javers/javers/v7.11.8/javers-core/src/main/java/org/javers/repository/api/JaversRepository.java): `List<CdoSnapshot>`, `Optional<CdoSnapshot>`, `void persist`, `void ensureSchema` 등 동기 메서드만 제공한다.
- [JaVers repository configuration](https://javers.org/documentation/repository-configuration/): SQL repository가 JDBC `ConnectionProvider`를 사용하고 애플리케이션 transaction이 connection을 소유할 수 있음을 설명한다. 지원 저장소 목록에는 MongoDB, H2, PostgreSQL, MySQL/MariaDB, Oracle, MSSQL, Redis가 기재되어 있으며 R2DBC adapter는 확인되지 않았다. 이 마지막 판정은 문서와 7.11.8 SPI를 함께 검토한 결과이다.
- [Exposed transactions](https://www.jetbrains.com/help/exposed/transactions.html): JDBC `transaction`은 현재 thread에서 동기 실행되고, coroutine 기반 non-blocking 경계에는 `suspendTransaction()`을 사용한다.
- [Exposed database connections](https://www.jetbrains.com/help/exposed/working-with-database.html): JDBC `Database`와 R2DBC `R2dbcDatabase`가 별도 연결 모델이다.
- [Exposed dependencies](https://www.jetbrains.com/help/exposed/adding-dependencies.html): `exposed-r2dbc`는 별도 모듈이며 DAO와 호환되지 않는다. 따라서 현재 DAO/JDBC mapping을 단순히 R2DBC transport로 바꿀 수 없다.
- [Spring Data R2DBC](https://docs.spring.io/spring-data/relational/reference/r2dbc.html): `R2dbcEntityTemplate`과 repository interface를 제공하지만, JaVers의 동기 repository SPI를 async로 바꾸지는 않는다.

## 후보별 판정

| 후보 | 판정 | 이유 |
| --- | --- | --- |
| 현재 SPI 뒤에 직접 R2DBC 구현 | **REJECT** | 동기 JaVers 호출자가 결과를 즉시 요구하므로 진정한 non-blocking 계약을 만족할 수 없다. `getAll()`/lock/head/sequence까지 함께 바꿔야 한다. |
| Exposed R2DBC adapter | **DEFER** | transport 자체는 적합하지만 async JaVers facade, schema/query mapping, transaction context, concurrency/head 일관성을 함께 설계해야 한다. |
| Spring Data R2DBC adapter | **DEFER** | template/repository는 제공되지만 JaVers CDO schema와 query semantics를 다시 매핑해야 하며 동일한 sync SPI 문제가 남는다. |
| 기존 JDBC + blocking dispatcher | **RECOMMEND (임시 완화)** | 현재 의미를 보존하면서 event-loop 오염을 caller 경계에서 제한한다. R2DBC 지원 또는 저장소 자체의 non-blocking 보장은 아니다. |

## 향후 구현으로 전환할 조건

다음 수요가 확인될 때에만 별도 후속 이슈를 만든다.

1. **API/facade**: `suspend` 또는 Reactive Streams 반환형으로 JaVers 호출을
   감싸고, 기존 `JaversRepository`와의 호환/호출 위치를 명시한다.
2. **transaction 계약**: caller-owned transaction, nested call, 실패 시
   rollback, connection/context 전파를 coroutine과 reactive 양쪽에서
   정의한다.
3. **adapter**: Exposed R2DBC 또는 direct driver 중 하나를 선택해
   commit, snapshot, head, sequence, schema, query filtering을 모두
   구현한다. 일부 쓰기 경로만 async로 만드는 것은 허용하지 않는다.
4. **동시성/정합성**: 동일 commit의 head/sequence 경쟁, 재시작 후 head 복원,
   `getAll()` 규모 제한과 pagination/back-pressure를 검증한다.
5. **검증 매트릭스**: 최소 PostgreSQL Testcontainers와 H2 대체 경로,
   coroutine cancellation/timeout, transaction rollback, concurrent commit,
   query order/limit/skip를 순차 테스트한다. 기존 JDBC 회귀 테스트도 함께
   유지한다.
6. **문서·성능**: Ktor/WebFlux 사용 예, blocking dispatcher와의 차이,
   throughput/latency/connection pool benchmark를 별도 문서에 기록한다.

## 이번 이슈의 변경 범위

- R2DBC runtime/module/dependency/adapter 코드는 추가하지 않는다.
- 현재 Exposed JDBC adapter의 동기 계약과 문서 경계를 유지한다.
- 이 문서가 build/defer/reject 판단과 후속 이슈 분할 기준을 제공한다.

## Assets

외부 이미지나 다운로드 파일은 사용하지 않았다.

## DoD Status

- [x] JaVers SPI, 현재 로컬 adapter, Exposed/Spring Data R2DBC 선택지를 근거와 함께 조사
- [x] build/defer/reject 판정과 transaction/query/test 영향 기록
- [x] 이번 이슈에서 구현하지 않을 범위와 후속 구현 조건 명시
- [ ] 별도 async API/adapter 구현 이슈 — 실제 수요 확인 후 생성
