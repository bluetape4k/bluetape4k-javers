# Issue 140 7-Tier Code Review

## Reviewed Scope

- New module: `examples/javers-spring-boot4`
- Example project rename: `:examples-javers-exposed-ddd`
- New example project: `:examples-javers-spring-boot4`
- Gradle publish-exclusion helper: `Project.isExampleProject()`
- Root/module README locale set and repo-local `AGENTS.md`
- CI and Nightly workflow registration
- Spec and plan conformance for Issue #140

## Verification Evidence

- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain` — passed; listed `:examples-javers-exposed-ddd` and `:examples-javers-spring-boot4`.
- `./gradlew :examples-javers-exposed-ddd:test :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `./gradlew :examples-javers-spring-boot4:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain` — passed.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` — passed.
- `rg -n -F "\\'" .github/workflows` — no escaped single quote matches.
- `git diff --check` — passed.
- Production concurrency scan on `examples/javers-spring-boot4/src/main/kotlin` for `GlobalScope`, `runBlocking`, `Thread.sleep`, `delay`, `synchronized`, `@Synchronized`, and `runCatching` — zero matches.
- Assertion/deprecated import scan on touched example code for `!!`, `SqlExpressionBuilder.eq`, JUnit/AssertJ assertions — zero matches.

## Tier Review

| Tier | Area | P0 | P1 | P2 | P3 | Evidence |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Request DTOs use Bean Validation; no secrets, auth boundary, or deserialization of arbitrary polymorphic types. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 1 | H2 schema initializes at startup through `InitializingBean`; no background clients. P3: example has no custom health/readiness endpoint, acceptable for example scope. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | Production module APIs are unchanged; example projects use `examples-javers-*` names and remain excluded by `isExampleProject()`. |
| 4 | Kotlin/Code Quality | 0 | 0 | 0 | 0 | DTOs implement `Serializable` and define `serialVersionUID`; public classes/DTOs have English KDoc; Exposed imports use top-level `eq`. |
| 5 | Tests/Types/Silent Failure | 0 | 0 | 0 | 0 | Tests cover create, paid transition, lookup, history metadata, 404, invalid payload, and history cap. Assertions use bluetape4k assertion extensions. |
| 6 | Performance/Stability | 0 | 0 | 0 | 1 | History limit is capped at 100; no suspend/event-loop blocking. P3: example uses synchronous JDBC, documented as expected for JDBC/Spring MVC and suitable for virtual-thread runtime usage. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locale set, AGENTS, CI/Nightly, Kover artifact, spec, plan, research, and lesson coverage are assigned. |

## Resolved Findings

| Priority | Tier | Finding | Resolution |
|---|---|---|---|
| P1 | 7 | Example project naming was initially `:javers-spring-boot4`, which would not support future `examples-javers-*` publish exclusion by project name. | Renamed new project to `:examples-javers-spring-boot4`; also renamed existing example project to `:examples-javers-exposed-ddd` and updated workflows/docs. |
| P1 | 4 | Spring Boot 4 test import used Boot 3 `AutoConfigureMockMvc` package and `TestRestTemplate`, which did not compile on Boot 4. | Switched to Boot 4 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` and added `spring-boot-starter-webmvc-test`. |
| P1 | 4 | Jackson dependency initially used Jackson 2 `com.fasterxml.jackson.module:jackson-module-kotlin`, while Boot 4 provides Jackson 3. | Switched to `tools.jackson.module:jackson-module-kotlin` and `tools.jackson.databind.ObjectMapper`. |
| P2 | 7 | Running the existing benchmark test rewrote benchmark metrics while the intended change was only the Gradle command name. | Preserved existing benchmark metric values and changed only the stored command string. |

## Final Gate

- P0 = 0
- P1 = 0
- Status: pass
