# Issue 141 Ktor JaVers Example Plan

참조 spec:
`docs/superpowers/specs/2026-06-04-issue-141-ktor-example-design.md`

## 복잡도

Type A full feature. 이 작업은 새 Ktor example module, explicit Exposed and JaVers
wiring, Ktor tests, README locale content, CI/Nightly registration을 추가한다.

## 작업 단계

1. 새 module을 등록한다.
   - `settings.gradle.kts`에 `:examples-javers-ktor`를 추가한다.
   - project directory를 `examples/javers-ktor`에 매핑한다.
   - `examples-javers-*` project-name prefix를 유지한다.
   - 기존 `isExampleProject()`가 publishing에서 이를 제외하는지 확인한다.

2. Gradle dependency를 추가한다.
   - `application` 및 Kotlin serialization plugin을 적용한다.
   - 현재 repository module `:javers-ddd`, `:javers-exposed`를 사용한다.
   - accessor가 compile되면 `ktor-bom`, `bluetape4k-ktor-core`, `bluetape4k-ktor-testing`에 central `bt4k` catalog를 사용한다.
   - local version duplication 없이 Ktor artifact coordinate를 추가한다.
     `ktor-server-core`, `ktor-server-cio`, `ktor-server-test-host`,
     `ktor-server-content-negotiation`, and `ktor-serialization-kotlinx-json`.
   - H2를 runtime/test database로 유지한다.

3. example domain과 persistence를 추가한다.
   - `:examples-javers-spring-boot4`의 order domain을 copy/adapt한다.
   - `OrdersTable`과 `OrderRepository`를 copy/adapt한다.
   - `eq` 같은 top-level Exposed operator를 보존한다.
   - persistence는 blocking 및 transaction-scoped로 유지한다. suspend repository API는 추가하지 않는다.

4. Ktor application wiring을 추가한다.
   - `javersKtorModule()`과 `main()`을 추가한다.
   - `installBluetape4kKtorCore()`를 install한다.
   - `CommitTable`, `CdoSnapshotTable`, `OrdersTable`용 H2/Exposed schema를 initialize한다.
   - `ExposedCdoSnapshotRepository`, `Javers`, `OrderRepository`, `OrderCommandHandler`를 명시적으로 만든다.
   - DI framework 또는 production auto-configuration은 도입하지 않는다.

5. Ktor route와 DTO를 추가한다.
   - `kotlinx.serialization.Serializable`을 사용하는 DTO를 추가한다.
   - endpoint를 추가한다.
     - `POST /orders`
     - `POST /orders/{orderId}/paid`
     - `GET /orders/{orderId}`
     - `GET /orders/{orderId}/history`
   - non-blank IDs/author/SKU, positive quantity, positive unit price, history limit에는 route-level validation을 사용한다.
   - unknown order는 `404`로, invalid request/state transition은 `400`으로 변환한다.

6. tests를 추가한다.
   - Ktor `testApplication`을 사용한다.
   - 맞는 곳에는 `bluetape4kJsonClient()`, `decodeJsonBody()`, `shouldHaveStatus()`를 사용한다.
   - create, pay, lookup, history metadata, unknown lookup, invalid payload, history cap, `/healthz`, `/readyz`를 커버한다.
   - `junit-platform.properties`와 `logback-test.xml`을 추가한다.

7. docs와 repo registration을 갱신한다.
   - module `README.md`와 `README.ko.md`를 추가한다.
   - root `README.md`와 `README.ko.md`를 갱신한다.
   - repo-local `AGENTS.md`를 갱신한다.
   - CI path filter, test job, status dependency, artifact를 추가한다.
   - Nightly test job, Kover XML command, coverage artifact, coverage aggregation `needs`, nightly status `needs`를 추가한다.

8. local에서 검증한다.
   - `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew :examples-javers-ktor:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
   - `./gradlew :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
   - `rg -n -F "\\'" .github/workflows`
   - `git diff --check`

9. Step 6-R 7-Tier code review를 실행한다.
   - 새 Ktor example module slice를 review한다.
   - root registration/docs/workflow slice를 review한다.
   - Ktor/JDBC blocking boundary docs를 확인한다.
   - assertion style, Exposed import style, serializer risk, workflow YAML, CI/Nightly coverage registration을 확인한다.
   - 모든 P0/P1 finding을 수정하고 영향받은 check를 재실행한다.

10. delivery를 마무리한다.
    - `docs/lessons/2026-06-04-issue-141-ktor-example.md`를 추가한다.
    - Lore trailer로 commit한다.
    - branch를 push한다.
    - assignee `debop`, milestone `0.3.0`으로 PR을 만든다.
    - PR review와 CI 이후 Step DoD status로 PR body를 갱신한다.
    - explicit user approval 없이 merge하지 않는다.

## Risk Controls

| 위험 | 제어 |
|---|---|
| Ktor accessor mismatch | dependency edit 직후 compile하고 central catalog accessor만 조정한다. |
| Blocking JDBC in Ktor | repository를 synchronous로 유지하고 boundary를 문서화하며, 이를 high-concurrency production guidance처럼 보이게 하지 않는다. |
| rich value type의 serialization failure | 단순 serializable response DTO field를 선호하고 Ktor JSON tests로 검증한다. |
| Hidden production API change | 모든 code를 새 example package 안에 유지하고 production module 수정을 피한다. |
| Workflow syntax regression | `actionlint`와 escaped single quote scan을 실행한다. |
| Assertion style drift | bluetape4k assertion/test helper만 사용한다. |

## Step 3-R 7-Tier 계획 검토

검토 범위:

- Plan tasks above
- Spec acceptance criteria
- Step 3-R plan review references
- repo-local module registration rules
- Issue #140 lesson on `examples-javers-*` naming

| Tier | 영역 | P0 | P1 | P2 | P3 | 증거 |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | validation, 400, 404 task가 assigned됐다. auth/secrets surface는 도입하지 않는다. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | schema initialization, health/readiness, no background client lifecycle이 explicit task다. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | production API는 변경하지 않는다. module registration은 implementation보다 먼저 수행된다. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Ktor helper reuse, serialization DTO, Exposed import rule이 명시적이다. |
| 5 | Tests/Types/Silent Failure | 0 | 0 | 0 | 0 | 각 route behavior, error path, health route, history cap에 named test task가 있다. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | JDBC blocking boundary는 documentation 및 review task다. suspend API는 추가하지 않는다. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locales, AGENTS, settings, CI, Nightly, Kover, lesson, PR, CI, DoD가 assigned됐다. |

### Iteration Log

- Iteration 1에서 두 개의 blocking plan gap을 식별했다.
  - P1/Tier 5: health/readiness tests가 assigned되지 않았다.
  - P1/Tier 7: Nightly coverage aggregation `needs`가 명시적으로 assigned되지 않았다.
- plan edit를 적용했다.
  - `/healthz`와 `/readyz` tests를 추가했다.
  - Nightly coverage/status `needs` update를 추가했다.
- Final gate: `P0 = 0`, `P1 = 0`.
