# Issue 141 Ktor JaVers Example 설계

## 맥락

Issue #141은 current `bluetape4k-javers` feature set용 Ktor example application을 추가한다. Issue #140은 이미 Spring Boot 4 counterpart를 추가했고 example Gradle project name이 `:examples-javers-*`로 시작해야 한다는 repository convention을 고쳤다.

이 slice는 non-Spring user가 pending Redis/Kafka/pipeline feature에 의존하지 않고 Ktor application에서 Exposed command persistence, `ExposedCdoSnapshotRepository`, JaVers commit, `javers-ddd` aggregate helper를 wire할 수 있음을 증명해야 한다.

## 목표

- `examples/javers-ktor`를 Ktor example module로 추가한다.
- 이를 `:examples-javers-ktor`로 등록한다.
- 현재 `javers-exposed` 및 `javers-ddd` API를 explicit wiring으로 사용한다.
- Exposed JDBC와 H2를 통해 order command state를 persist한다.
- `ExposedCdoSnapshotRepository`를 통해 JaVers snapshot을 persist한다.
- Command, lookup, audit-history flow용 Ktor route를 제공한다.
- 맞는 곳에서는 `bluetape4k-ktor-core`와 `bluetape4k-ktor-testing`을 재사용한다.
- Focused Ktor `testApplication` integration test를 추가한다.
- Module을 settings, README locale set, repo module list, CI, Nightly coverage에 등록한다.

## Non-goal

- 새 JaVers repository abstraction을 추가하지 않는다.
- Redis projection endpoint를 추가하지 않는다.
- Kafka/NATS/SQS pipeline 또는 vanilla Kafka adapter를 추가하지 않는다.
- Spring Boot auto-configuration을 추가하지 않는다.
- Production-grade auth, outbox, retry, metrics, deployment guide를 추가하지 않는다.
- Production module public API를 변경하지 않는다.

## Module 형태

| 영역 | 설계 |
|---|---|
| Module path | `examples/javers-ktor` |
| Gradle project | `:examples-javers-ktor` |
| Package | `io.bluetape4k.javers.examples.ktor` |
| Runtime | Ktor 3, CIO, Exposed JDBC, H2 |
| Ktor helpers | `bluetape4k-ktor-core`, `bluetape4k-ktor-testing` |
| JaVers storage | `ExposedCdoSnapshotRepository` |
| Aggregate support | `javers-ddd` `AggregateRepository`, `AggregateRoot`, `DomainEvent` |
| Tests | Ktor `testApplication` with JSON client |

## Route 계약

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
- Unknown order lookup 및 history lookup은 `404`를 반환한다.
- Invalid request payload 또는 illegal state transition은 bluetape4k Ktor core error response를 통해 `400`을 반환한다.
- Response DTO는 practical한 범위에서 serializer-heavy value type을 피한다. Example이 custom JSON serializer보다 JaVers에 집중되도록 monetary 및 timestamp value는 string으로 노출할 수 있다.

## Persistence 및 Wiring 설계

- `OrdersTable`은 command-side order state를 저장한다.
- `OrderRepository`는 Exposed transaction에서 command state를 persist하고 JaVers commit을 `AggregateRepository`에 위임한다.
- `javersKtorModule()`은 다음을 생성한다.
  - H2-backed Exposed `Database`
  - schema initialization for order and JaVers tables
  - `ExposedCdoSnapshotRepository`
  - `Javers`
  - `OrderRepository`
  - `OrderCommandHandler`
  - Ktor route registration
- Default module은 example과 test를 위해 self-contained다. Dependency injection framework를 도입하지 않는다.

## Ktor/JDBC Blocking Boundary

Example은 current JaVers Exposed repository가 JDBC-backed이므로 의도적으로 synchronous Exposed JDBC를 사용한다. 이는 example에는 허용 가능하고 virtual-thread-friendly JVM runtime experiment에도 맞지만, production event-loop blocking recommendation으로 제시하지 않는다. README는 production Ktor deployment가 high concurrency에서 같은 형태를 사용하기 전에 worker dispatcher isolation, virtual-thread execution, future R2DBC path를 고려해야 한다고 명시해야 한다.

## Test 설계

Focused Ktor integration test는 다음을 증명해야 한다.

- `POST /orders`는 command state를 persist하고 JaVers snapshot 하나를 생성한다.
- `POST /orders/{orderId}/paid`는 state를 update하고 두 번째 snapshot을 생성한다.
- `GET /orders/{orderId}`는 current state를 반환한다.
- `GET /orders/{orderId}/history`는 domain event type metadata가 있는 bounded history를 반환한다.
- Unknown order lookup은 `404`를 반환한다.
- Invalid create payload는 `400`을 반환한다.
- Invalid history limit은 100으로 cap된다.
- `bluetape4k-ktor-core`의 `/healthz` 및 `/readyz`를 사용할 수 있다.

Test는 JUnit/AssertJ assertion 대신 bluetape4k assertion extension과 Ktor test helper를 사용해야 한다.

## 문서 및 등록

- Module-level `README.md` 및 `README.ko.md`를 추가한다.
- Root `README.md` 및 `README.ko.md`를 갱신한다.
- Repo-local `AGENTS.md`를 갱신한다.
- `settings.gradle.kts`에 module을 등록한다.
- CI path filter와 test job을 추가한다.
- Nightly test job, coverage command, coverage artifact, aggregation dependency를 추가한다.

## 인수 기준

- `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  가 통과한다.
- `./gradlew :examples-javers-ktor:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  가 통과한다.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
  가 `:examples-javers-ktor`를 list한다.
- `./gradlew :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  가 통과한다.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
  가 available할 때 통과한다.
- `rg -n -F "\\'" .github/workflows`가 match를 반환하지 않는다.
- `git diff --check`가 통과한다.
- Spec, plan, code review가 각각 `P0 = 0` 및 `P1 = 0`인 7-Tier review를 기록한다.

## 검토한 대안

- Spring Boot 4 controller 직접 재사용: Issue #141은 non-Spring Ktor wiring을 보여주기 위한 것이므로 기각한다.
- Redis projection 또는 Kafka publishing endpoint 추가: 이는 별도 future feature issue에서 추적되며 acceptance를 흐리므로 기각한다.
- Shared example-domain module 추가: example-local duplication을 줄이기 위해 또 다른 module과 dependency edge를 도입하므로 이 slice에서는 기각한다.
- Full domain DTO shape용 custom JSON serializer 사용: compile/test가 요구하지 않는 한 기각한다. String response field만으로 audit behavior를 증명하기에 충분하다.

## Step 2-R 7-Tier Spec 검토

검토 범위:

- `docs/superpowers/research/2026-06-04-issue-141-ktor-example-research.md`
- `docs/superpowers/specs/2026-06-04-issue-141-ktor-example-design.md`
- Issue #141 requirements
- Ktor official documentation evidence from Context7
- `bluetape4k-projects` Ktor helper examples
- repo-local `AGENTS.md` module registration rules

| Tier | 영역 | P0 | P1 | P2 | P3 | 근거 |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Auth/secret boundary를 추가하지 않는다. Caller input validation과 400/404 contract가 지정됐다. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Example-local H2/Exposed startup이 explicit하다. `bluetape4k-ktor-core` health/readiness route가 required다. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | 새 example module만 추가하며 production module API는 변경하지 않는다. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Ktor helper reuse와 example-local package boundary가 지정됐다. 새 shared abstraction은 없다. |
| 5 | Testability/Types/Silent Failure | 0 | 0 | 0 | 0 | Success, failure, lookup, history, cap, health test가 명명됐다. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | JDBC blocking boundary가 explicit하고 문서화되어야 한다. History limit은 cap된다. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locale set, AGENTS, settings, CI, Nightly, Kover, workflow check가 required다. |

### Iteration Log

- Iteration 1은 finalize 전에 blocking spec gap 세 개를 식별했다.
  - P1/Tier 2: health/readiness route expectation이 explicit하지 않았다.
  - P1/Tier 5: invalid payload 및 history cap test가 명명되지 않았다.
  - P1/Tier 6: Ktor event-loop blocking risk가 문서화되지 않았다.
- 적용한 spec edit:
  - `/healthz` 및 `/readyz` acceptance 추가.
  - Invalid payload 및 history cap test requirement 추가.
  - Ktor/JDBC blocking boundary section 추가.
- 최종 gate: `P0 = 0`, `P1 = 0`.
