# Issue 140 7-Tier 코드 검토

## 검토 범위

- 새 module: `examples/javers-spring-boot4`
- Example project rename: `:examples-javers-exposed-ddd`
- 새 example project: `:examples-javers-spring-boot4`
- Gradle publish-exclusion helper: `Project.isExampleProject()`
- Root/module README locale set and repo-local `AGENTS.md`
- CI and Nightly workflow registration
- Spec and plan conformance for Issue #140

## 검증 증거

- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain` — passed; `:examples-javers-exposed-ddd`와 `:examples-javers-spring-boot4`를 listed.
- `./gradlew :examples-javers-exposed-ddd:test :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew :examples-javers-spring-boot4:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` — passed.
- `rg -n -F "\\'" .github/workflows` — escaped single quote match 없음.
- `git diff --check` — passed.
- `examples/javers-spring-boot4/src/main/kotlin`에서 `GlobalScope`, `runBlocking`, `Thread.sleep`, `delay`, `synchronized`, `@Synchronized`, `runCatching`에 대한 production concurrency scan — zero matches.
- touched example code에서 `!!`, `SqlExpressionBuilder.eq`, JUnit/AssertJ assertions에 대한 assertion/deprecated import scan — zero matches.

## Tier 검토

| Tier | 영역 | P0 | P1 | P2 | P3 | 증거 |
|---|---|---:|---:|---:|---:|---|
| 1 | 보안 | 0 | 0 | 0 | 0 | Request DTO는 Bean Validation을 사용한다. secret, auth boundary, arbitrary polymorphic type deserialization은 없다. |
| 2 | 운영/SRE 신뢰성 | 0 | 0 | 0 | 1 | H2 schema는 startup에서 `InitializingBean`을 통해 initialize된다. background client는 없다. P3: example에는 custom health/readiness endpoint가 없지만 example scope에서는 허용 가능하다. |
| 3 | 구조 영향 | 0 | 0 | 0 | 0 | Production module API는 변경되지 않았다. example project는 `examples-javers-*` name을 사용하며 `isExampleProject()`로 계속 제외된다. |
| 4 | Kotlin/코드 품질 | 0 | 0 | 0 | 0 | DTO는 `Serializable`을 구현하고 `serialVersionUID`를 정의한다. public class/DTO는 English KDoc을 가진다. Exposed import는 top-level `eq`를 사용한다. |
| 5 | 테스트/타입/무음 실패 | 0 | 0 | 0 | 0 | Tests는 create, paid transition, lookup, history metadata, 404, invalid payload, history cap을 커버한다. Assertions는 bluetape4k assertion extension을 사용한다. |
| 6 | 성능/안정성 | 0 | 0 | 0 | 1 | History limit은 100으로 capped된다. suspend/event-loop blocking은 없다. P3: example은 synchronous JDBC를 사용하며, JDBC/Spring MVC에서 예상되는 동작이고 virtual-thread runtime usage에 적합하다고 문서화했다. |
| 7 | 문서/릴리스/증거 | 0 | 0 | 0 | 0 | README locale set, AGENTS, CI/Nightly, Kover artifact, spec, plan, research, lesson coverage가 assigned됐다. |

## 해결된 Findings

| Priority | Tier | 결과 | 해결 |
|---|---|---|---|
| P1 | 7 | Example project naming이 처음에는 `:javers-spring-boot4`라서 future `examples-javers-*` publish exclusion을 project name 기준으로 지원하지 못했다. | 새 project를 `:examples-javers-spring-boot4`로 rename했다. 기존 example project도 `:examples-javers-exposed-ddd`로 rename하고 workflows/docs를 갱신했다. |
| P1 | 4 | Spring Boot 4 test import가 Boot 3 `AutoConfigureMockMvc` package와 `TestRestTemplate`을 사용해 Boot 4에서 compile되지 않았다. | Boot 4 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`로 전환하고 `spring-boot-starter-webmvc-test`를 추가했다. |
| P1 | 4 | Boot 4가 Jackson 3을 제공하는데 Jackson dependency가 처음에는 Jackson 2 `com.fasterxml.jackson.module:jackson-module-kotlin`을 사용했다. | `tools.jackson.module:jackson-module-kotlin`과 `tools.jackson.databind.ObjectMapper`로 전환했다. |
| P2 | 7 | 의도한 변경은 Gradle command name뿐이었지만 기존 benchmark test 실행이 benchmark metric을 다시 썼다. | 기존 benchmark metric value를 보존하고 저장된 command string만 변경했다. |

## 최종 Gate

- P0 = 0
- P1 = 0
- Status: pass
