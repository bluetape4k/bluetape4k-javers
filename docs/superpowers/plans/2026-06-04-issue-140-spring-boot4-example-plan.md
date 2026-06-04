# Issue 140 Spring Boot 4 JaVers Example Plan

Reference spec:
`docs/superpowers/specs/2026-06-04-issue-140-spring-boot4-example-design.md`

## Complexity

Type A full feature. The work adds a new example module, Spring Boot 4 runtime
wiring, REST tests, README locale content, and CI/Nightly registration.

## Work Steps

1. Register the new module.
   - Add `:examples-javers-spring-boot4` in `settings.gradle.kts`.
   - Map project directory to `examples/javers-spring-boot4`.
   - Use the `examples-javers-*` project-name prefix so future publish filters
     can exclude example projects by prefix.
   - Keep the module example-scoped and unpublished through existing
     `isExampleProject()` behavior.

2. Add Gradle dependencies.
   - Use current repository modules: `:javers-ddd`, `:javers-exposed`.
   - Use central `bt4k` catalog aliases for Spring Boot 4 dependencies when
     accessors are available.
   - Add Spring MVC/test dependencies without duplicating centrally governed
     Spring Boot versions.
   - Keep H2 as the runtime/test database.

3. Add example domain and persistence.
   - Copy/adapt the existing order domain from `:examples-javers-exposed-ddd`.
   - Copy/adapt `OrdersTable` and `OrderRepository`.
   - Preserve top-level Exposed operators such as `eq`.
   - Keep persistence blocking and transaction-scoped; no suspend APIs are added.

4. Add Spring Boot 4 application wiring.
   - Add application entrypoint.
   - Add explicit configuration for `Database`, schema initialization,
     `ExposedCdoSnapshotRepository`, `Javers`, `OrderRepository`, and
     `OrderCommandHandler`.
   - Add a fixed clock bean for deterministic tests only through test
     configuration or overridable production bean.
   - Avoid introducing Spring Boot auto-configuration.

5. Add REST API.
   - Add request/response DTOs with Bean Validation annotations.
   - Add controller endpoints:
     - `POST /orders`
     - `POST /orders/{orderId}/paid`
     - `GET /orders/{orderId}`
     - `GET /orders/{orderId}/history`
   - Enforce `history.limit` default 20 and cap 100.
   - Return `404` for unknown order lookup.

6. Add tests.
   - Use Spring Boot MVC integration tests with `MockMvc`.
   - Use bluetape4k assertion extensions in assertions.
   - Cover create, pay, lookup, history, unknown lookup, invalid payload, and
     limit cap behavior.
   - Add `junit-platform.properties` and `logback-test.xml`.

7. Update docs and repo registration.
   - Add module `README.md` and `README.ko.md`.
   - Update root `README.md` and `README.ko.md`.
   - Update repo-local `AGENTS.md`.
   - Add CI path filter, test job, status dependency, and test artifact.
   - Add Nightly test job, coverage command, coverage artifact, and coverage
     aggregation dependency.

8. Verify locally.
   - `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
   - `git diff --check`
   - `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
   - `rg -n "\\\\'" .github/workflows` must return no matches.

9. Run Step 6-R 7-Tier code review.
   - Review the new example module slice.
   - Review root registration/docs/workflow slice.
   - Fix all P0/P1 findings and rerun affected checks.

10. Finish delivery.
    - Add `docs/lessons/2026-06-04-issue-140-spring-boot4-example.md`.
    - Commit with Lore trailers.
    - Push branch.
    - Create PR assigned to `debop` with milestone `0.3.0` when available.
    - Do not merge without explicit user approval.

## Risk Controls

| Risk | Control |
|---|---|
| Spring Boot accessor mismatch | Compile early after dependency edit and adjust to central catalog alias names. |
| Runtime schema race in tests | Initialize schema through a startup bean before REST tests call endpoints. |
| Unbounded history response | Controller clamps limit to 100 and tests verify cap behavior. |
| Hidden public API change | New code stays inside example module package; production modules are not changed. |
| Workflow syntax regression | Run `actionlint` if installed and escaped-quote scan always. |
| Assertion style drift | Use bluetape4k assertions, not AssertJ/JUnit assertions. |

## Step 3-R 7-Tier Plan Review

Reviewed scope:

- Plan tasks above
- Spec acceptance criteria
- Step 3-R plan review checklist
- repo-local module registration rules

| Tier | Area | P0 | P1 | P2 | P3 | Evidence |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Validation and 404 behavior are assigned; no secret/auth surface is introduced. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Startup schema initialization and no background client lifecycle are explicit. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | Production module APIs are not changed; registration tasks are ordered first. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Exposed deprecated import avoidance and assertion style are explicit tasks. |
| 5 | Tests/Types/Silent Failure | 0 | 0 | 0 | 0 | Every REST behavior and history cap has a named test task. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | Blocking JDBC remains synchronous; no suspend/event-loop API is introduced. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locales, AGENTS, CI, Nightly, lesson, commit, PR, and no-merge rule are assigned. |

### Iteration Log

- Iteration 1 identified two blocking plan gaps:
  - P1/Tier 2: schema initialization lifecycle was not assigned.
  - P1/Tier 5: invalid payload and history cap tests were not named.
- Plan edits applied:
  - Added explicit schema initialization task.
  - Added invalid payload and limit cap test tasks.
- Final gate: `P0 = 0`, `P1 = 0`.
