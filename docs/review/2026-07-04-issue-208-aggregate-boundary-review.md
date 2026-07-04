# Issue #208 aggregate save boundary review

## Scope

- Add a transaction-aware save boundary hook to `AggregateRepository`.
- Run Exposed-backed DDD example repositories inside that boundary.
- Keep event publication after source persistence and JaVers audit commit.
- Add failure-injection coverage for JaVers commit failure and publisher failure.

## 7-Tier Review

| Tier | Verdict | Evidence |
|---|---|---|
| 1. Correctness | PASS | `saveAuditBoundary` wraps `persist` and `javers.commit`; Exposed repositories override it with `transaction(database)`. |
| 2. API and compatibility | PASS | The new hook is `protected open`, so public save/load API remains unchanged. |
| 3. Transaction semantics | PASS | Source row and JaVers audit commit share the Exposed transaction for the example repositories; publisher execution remains after that boundary. |
| 4. Failure behavior | PASS | Regression tests prove JaVers commit failure rolls back the source row and publisher failure leaves committed source/audit state. |
| 5. Tests and silent failure | PASS | `examples-javers-exposed-ddd:test` now executes 7 tests including both failure paths. |
| 6. Documentation/KDoc | PASS | `AggregateRepository` KDoc documents the transaction-aware boundary contract. |
| 7. Release maintainability | PASS | Ktor and Spring Boot 4 copied Exposed repositories use the same boundary override, avoiding example drift. |

## Validation

- `./gradlew :javers-ddd:compileKotlin :examples-javers-exposed-ddd:compileKotlin :examples-javers-ktor:compileKotlin :examples-javers-spring-boot4:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 7 tests.
- `./gradlew :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 11 tests.
- `git diff --check`
  - Result: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
