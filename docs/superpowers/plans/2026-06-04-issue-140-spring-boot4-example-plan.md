# Issue 140 Spring Boot 4 JaVers Example Plan

참조 spec:
`docs/superpowers/specs/2026-06-04-issue-140-spring-boot4-example-design.md`

## 복잡도

Type A full feature. 이 작업은 새 example module, Spring Boot 4 runtime wiring,
REST tests, README locale content, CI/Nightly registration을 추가한다.

## 작업 단계

1. 새 module을 등록한다.
   - `settings.gradle.kts`에 `:examples-javers-spring-boot4`를 추가한다.
   - project directory를 `examples/javers-spring-boot4`에 매핑한다.
   - future publish filter가 prefix로 example project를 제외할 수 있도록 `examples-javers-*` project-name prefix를 사용한다.
   - 기존 `isExampleProject()` behavior를 통해 module을 example-scoped 및 unpublished 상태로 유지한다.

2. Gradle dependency를 추가한다.
   - 현재 repository module `:javers-ddd`, `:javers-exposed`를 사용한다.
   - accessor를 사용할 수 있으면 Spring Boot 4 dependency에는 central `bt4k` catalog alias를 사용한다.
   - centrally governed Spring Boot version을 중복하지 않고 Spring MVC/test dependency를 추가한다.
   - runtime/test database로 H2를 유지한다.

3. example domain과 persistence를 추가한다.
   - `:examples-javers-exposed-ddd`의 기존 order domain을 copy/adapt한다.
   - `OrdersTable`과 `OrderRepository`를 copy/adapt한다.
   - `eq` 같은 top-level Exposed operator를 보존한다.
   - persistence는 blocking 및 transaction-scoped로 유지한다. suspend API는 추가하지 않는다.

4. Spring Boot 4 application wiring을 추가한다.
   - application entrypoint를 추가한다.
   - `Database`, schema initialization, `ExposedCdoSnapshotRepository`, `Javers`, `OrderRepository`, `OrderCommandHandler`에 대한 explicit configuration을 추가한다.
   - deterministic tests 전용 fixed clock bean을 test configuration 또는 overridable production bean으로 추가한다.
   - Spring Boot auto-configuration은 도입하지 않는다.

5. REST API를 추가한다.
   - Bean Validation annotation이 있는 request/response DTO를 추가한다.
   - controller endpoint를 추가한다.
     - `POST /orders`
     - `POST /orders/{orderId}/paid`
     - `GET /orders/{orderId}`
     - `GET /orders/{orderId}/history`
   - `history.limit` default 20과 cap 100을 enforce한다.
   - unknown order lookup에는 `404`를 반환한다.

6. tests를 추가한다.
   - `MockMvc`를 사용하는 Spring Boot MVC integration tests를 사용한다.
   - assertion에는 bluetape4k assertion extension을 사용한다.
   - create, pay, lookup, history, unknown lookup, invalid payload, limit cap behavior를 커버한다.
   - `junit-platform.properties`와 `logback-test.xml`을 추가한다.

7. docs와 repo registration을 갱신한다.
   - module `README.md`와 `README.ko.md`를 추가한다.
   - root `README.md`와 `README.ko.md`를 갱신한다.
   - repo-local `AGENTS.md`를 갱신한다.
   - CI path filter, test job, status dependency, test artifact를 추가한다.
   - Nightly test job, coverage command, coverage artifact, coverage aggregation dependency를 추가한다.

8. local에서 검증한다.
   - `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
   - `git diff --check`
   - `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
   - `rg -n "\\\\'" .github/workflows`는 match를 반환하면 안 된다.

9. Step 6-R 7-Tier code review를 실행한다.
   - 새 example module slice를 review한다.
   - root registration/docs/workflow slice를 review한다.
   - 모든 P0/P1 finding을 수정하고 영향받은 check를 재실행한다.

10. delivery를 마무리한다.
    - `docs/lessons/2026-06-04-issue-140-spring-boot4-example.md`를 추가한다.
    - Lore trailer로 commit한다.
    - branch를 push한다.
    - 가능하면 milestone `0.3.0` 및 assignee `debop`으로 PR을 만든다.
    - explicit user approval 없이 merge하지 않는다.

## Risk Controls

| 위험 | 제어 |
|---|---|
| Spring Boot accessor mismatch | dependency edit 이후 일찍 compile하고 central catalog alias name에 맞춘다. |
| Runtime schema race in tests | REST tests가 endpoint를 호출하기 전에 startup bean을 통해 schema를 initialize한다. |
| Unbounded history response | Controller가 limit을 100으로 clamp하고 tests가 cap behavior를 검증한다. |
| Hidden public API change | 새 code는 example module package 안에 머문다. production module은 변경하지 않는다. |
| Workflow syntax regression | 설치되어 있으면 `actionlint`를 실행하고 escaped-quote scan은 항상 수행한다. |
| Assertion style drift | AssertJ/JUnit assertion 대신 bluetape4k assertion을 사용한다. |

## Step 3-R 7-Tier 계획 검토

검토 범위:

- Plan tasks above
- Spec acceptance criteria
- Step 3-R plan review checklist
- repo-local module registration rules

| Tier | 영역 | P0 | P1 | P2 | P3 | 증거 |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | validation과 404 behavior가 assigned됐다. secret/auth surface는 도입하지 않는다. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | startup schema initialization과 no background client lifecycle이 명시적이다. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | production module API는 변경하지 않는다. registration task가 먼저 정렬되어 있다. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Exposed deprecated import avoidance와 assertion style이 explicit task다. |
| 5 | Tests/Types/Silent Failure | 0 | 0 | 0 | 0 | 모든 REST behavior와 history cap에 named test task가 있다. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | blocking JDBC는 synchronous로 남는다. suspend/event-loop API는 도입하지 않는다. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locales, AGENTS, CI, Nightly, lesson, commit, PR, no-merge rule이 assigned됐다. |

### Iteration Log

- Iteration 1에서 두 개의 blocking plan gap을 식별했다.
  - P1/Tier 2: schema initialization lifecycle이 assigned되지 않았다.
  - P1/Tier 5: invalid payload와 history cap tests가 named되지 않았다.
- plan edit를 적용했다.
  - explicit schema initialization task를 추가했다.
  - invalid payload 및 limit cap test task를 추가했다.
- Final gate: `P0 = 0`, `P1 = 0`.
