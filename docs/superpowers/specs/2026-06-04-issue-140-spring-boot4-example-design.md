# Issue 140 Spring Boot 4 JaVers Example 설계

## 맥락

Issue #140은 current `bluetape4k-javers` feature set용 Spring Boot 4 example application을 추가한다. Repository에는 이미 Exposed command persistence, JaVers snapshot persistence, DDD aggregate helper, Kafka event, Redis projection을 증명하는 `examples/javers-exposed-ddd`가 있다.

이 slice는 future auto-configuration 또는 pending Redis/Kafka repository work에 의존하지 않고 HTTP application example을 추가한다.

## 목표

- `examples/javers-spring-boot4`를 Spring Boot 4 example module로 추가한다.
- 현재 `javers-exposed` 및 `javers-ddd` API를 explicit wiring으로 사용한다.
- Exposed JDBC와 H2를 통해 order command state를 persist한다.
- `ExposedCdoSnapshotRepository`를 통해 JaVers snapshot을 persist한다.
- Command, lookup, audit-history flow용 REST endpoint를 제공한다.
- Focused Spring Boot integration test를 추가한다.
- Module을 settings, README locale set, repo module list, CI, Nightly coverage에 등록한다.

## Non-goal

- 새 JaVers repository abstraction을 추가하지 않는다.
- Spring Boot auto-configuration module을 추가하지 않는다.
- Redis/Kafka/NATS/SQS pipeline implementation을 추가하지 않는다.
- Production-grade auth, outbox, retry, deployment guide를 추가하지 않는다.
- Published production module API를 변경하지 않는다.

## Module 형태

| 영역 | 설계 |
|---|---|
| Module path | `examples/javers-spring-boot4` |
| Gradle project | `:examples-javers-spring-boot4` |
| Package | `io.bluetape4k.javers.examples.springboot4` |
| Runtime | Spring Boot 4, Spring MVC, Exposed JDBC, H2 |
| JaVers storage | `ExposedCdoSnapshotRepository` |
| Aggregate support | `javers-ddd` `AggregateRepository`, `AggregateRoot`, `DomainEvent` |
| Tests | Spring Boot MVC integration tests with `MockMvc` |

## REST 계약

Example은 작은 order endpoint를 노출한다. ID는 caller-supplied string이므로 test가 deterministic history를 assert할 수 있다.

| Method | Path | 동작 |
|---|---|---|
| `POST` | `/orders` | Order를 place하고 첫 JaVers snapshot을 commit한다. |
| `POST` | `/orders/{orderId}/paid` | 기존 order를 paid로 mark하고 두 번째 snapshot을 commit한다. |
| `GET` | `/orders/{orderId}` | 현재 command-side order state 또는 `404`를 반환한다. |
| `GET` | `/orders/{orderId}/history?limit=20` | Newest-first JaVers snapshot metadata와 state를 반환한다. |

### Request/Response Rule

- `orderId`, `customerId`, `sku`, `author`는 non-blank여야 한다.
- Item quantity는 positive여야 한다.
- Item unit price는 positive여야 한다.
- `history.limit` default는 20이고 100으로 cap한다.
- Unknown order lookup은 `404`를 반환한다.
- Invalid request payload는 Spring Boot standard validation error response를 반환한다. Example은 custom exception infrastructure를 추가하지 않는다.

## Persistence 설계

- `OrdersTable`은 command-side order state를 저장한다.
- `OrderRepository`는 Exposed transaction 안에서 command state를 persist한 뒤 JaVers commit을 `AggregateRepository`에 위임한다.
- `JaversConfiguration`은 다음을 생성한다.
  - H2-backed Exposed `Database`
  - schema initializer for order and JaVers tables
  - `ExposedCdoSnapshotRepository`
  - `Javers`
  - `OrderRepository`
  - `OrderCommandHandler`
- Database setup은 example-local이며 explicit하다. Repository-wide Spring Boot auto-configuration을 암시하지 않는다.

## Test 설계

Focused `@SpringBootTest` + `MockMvc` test는 다음을 증명해야 한다.

- `POST /orders`는 command state를 persist하고 JaVers snapshot 하나를 생성한다.
- `POST /orders/{orderId}/paid`는 state를 update하고 두 번째 snapshot을 생성한다.
- `GET /orders/{orderId}`는 current state를 반환한다.
- `GET /orders/{orderId}/history`는 event type metadata가 있는 bounded history를 반환한다.
- Unknown order는 `404`를 반환한다.
- Invalid payload는 client error를 반환한다.

Test body에서는 bluetape4k assertion extension을 사용하고 새 test assertion에서 AssertJ/JUnit assertion API를 피해야 한다.

## 문서 및 등록

- Module-level `README.md` 및 `README.ko.md`를 추가한다.
- Root `README.md` 및 `README.ko.md`의 module table과 test command list를 갱신한다.
- Repo-local `AGENTS.md`의 module table과 command list를 갱신한다.
- `settings.gradle.kts`에 module을 등록한다.
- CI path filter와 test job을 추가한다.
- Nightly test job과 coverage artifact를 추가한다.

## 인수 기준

- `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  가 통과한다.
- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  이 통과한다.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
  가 `:examples-javers-spring-boot4`를 list한다.
- `git diff --check`가 통과한다.
- Tooling이 available하면 workflow syntax check가 통과한다.
- Spec, plan, code review가 각각 `P0 = 0` 및 `P1 = 0`인 7-Tier review를 기록한다.

## 검토한 대안

- `examples/javers-exposed-ddd`를 Spring Boot app으로 직접 재사용: 기존 example이 이미 Kafka/Redis CQRS flow를 소유하고 있어 Spring Boot example의 초점이 흐려지므로 기각한다.
- Redis projection endpoint 추가: Redis read-through/write strategy는 별도 feature issue에서 추적되고 Issue #140에 필요하지 않으므로 기각한다.
- Spring Boot auto-configuration 추가: Issue #140은 새 boot starter behavior가 아니라 current feature를 사용하는 example을 명시적으로 원하므로 기각한다.

## Step 2-R 7-Tier Spec 검토

검토 범위:

- `docs/superpowers/research/2026-06-04-issue-140-spring-boot4-example-research.md`
- `docs/superpowers/specs/2026-06-04-issue-140-spring-boot4-example-design.md`
- Issue #140 requirements
- repo-local `AGENTS.md` module registration rules

| Tier | 영역 | P0 | P1 | P2 | P3 | 근거 |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Example에는 auth boundary와 secret이 없다. Validation rule은 caller-controlled identifier와 numeric field를 cover한다. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Example-local H2/Exposed startup이 explicit하고 background client가 없다. Production retry/outbox는 non-goal이다. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | 새 example module만 추가하며 production public API change는 없다. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Kotlin data contract와 explicit package/module boundary가 지정됐다. Public KDoc 영향은 example-local이다. |
| 5 | Testability/Types/Silent Failure | 0 | 0 | 0 | 0 | Success, failure, lookup, bounded-history test가 명명됐다. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | History limit default와 cap이 지정됐다. Unbounded Redis/Kafka/Testcontainers loop가 없다. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locale set, AGENTS, CI, Nightly, coverage, Gradle projects check가 required다. |

### Iteration Log

- Iteration 1은 finalize 전에 blocking spec gap 두 개를 식별했다.
  - P1/Tier 5: history endpoint가 bounded result semantics를 지정하지 않았다.
  - P1/Tier 7: CI/Nightly coverage artifact registration이 explicit하지 않았다.
- 적용한 spec edit:
  - `history.limit` default/cap 추가.
  - 문서 및 인수 기준에 CI/Nightly coverage artifact registration 추가.
- 최종 gate: `P0 = 0`, `P1 = 0`.
