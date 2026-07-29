# Issue #208 aggregate save boundary review

## 범위

- `AggregateRepository`에 transaction-aware save boundary hook을 추가한다.
- Exposed-backed DDD example repository를 그 boundary 안에서 실행한다.
- event publication은 source persistence와 JaVers audit commit 이후에 유지한다.
- JaVers commit failure와 publisher failure에 대한 failure-injection coverage를 추가한다.

## 7-Tier 검토

| Tier | 판정 | 증거 |
|---|---|---|
| 1. Correctness | PASS | `saveAuditBoundary`는 `persist`와 `javers.commit`을 감싼다. Exposed repository는 `transaction(database)`로 override한다. |
| 2. API and compatibility | PASS | 새 hook은 `protected open`이므로 public save/load API는 변경되지 않는다. |
| 3. Transaction semantics | PASS | example repository에서 source row와 JaVers audit commit은 Exposed transaction을 공유한다. publisher execution은 해당 boundary 이후에 남는다. |
| 4. Failure behavior | PASS | Regression tests는 JaVers commit failure가 source row를 rollback하고 publisher failure는 committed source/audit state를 남김을 증명한다. |
| 5. Tests and silent failure | PASS | `examples-javers-exposed-ddd:test`는 이제 두 failure path를 포함해 7 tests를 실행한다. |
| 6. Documentation/KDoc | PASS | `AggregateRepository` KDoc은 transaction-aware boundary contract를 문서화한다. |
| 7. Release maintainability | PASS | Ktor 및 Spring Boot 4의 copied Exposed repository가 같은 boundary override를 사용해 example drift를 피한다. |

## 검증

- `./gradlew :javers-ddd:compileKotlin :examples-javers-exposed-ddd:compileKotlin :examples-javers-ktor:compileKotlin :examples-javers-spring-boot4:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 7 tests.
- `./gradlew :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 11 tests.
- `git diff --check`
  - 결과: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
