# Issue 141 Ktor JaVers Example Plan

Reference spec:
`docs/superpowers/specs/2026-06-04-issue-141-ktor-example-design.md`

## Complexity

Type A full feature. The work adds a new Ktor example module, explicit Exposed
and JaVers wiring, Ktor tests, README locale content, and CI/Nightly
registration.

## Work Steps

1. Register the new module.
   - Add `:examples-javers-ktor` in `settings.gradle.kts`.
   - Map project directory to `examples/javers-ktor`.
   - Keep the `examples-javers-*` project-name prefix.
   - Ensure existing `isExampleProject()` excludes it from publishing.

2. Add Gradle dependencies.
   - Apply `application` and Kotlin serialization plugin.
   - Use current repository modules: `:javers-ddd`, `:javers-exposed`.
   - Use central `bt4k` catalog for `ktor-bom`, `bluetape4k-ktor-core`, and
     `bluetape4k-ktor-testing` when accessors compile.
   - Add Ktor artifact coordinates without local version duplication:
     `ktor-server-core`, `ktor-server-cio`, `ktor-server-test-host`,
     `ktor-server-content-negotiation`, and `ktor-serialization-kotlinx-json`.
   - Keep H2 as runtime/test database.

3. Add example domain and persistence.
   - Copy/adapt the order domain from `:examples-javers-spring-boot4`.
   - Copy/adapt `OrdersTable` and `OrderRepository`.
   - Preserve top-level Exposed operators such as `eq`.
   - Keep persistence blocking and transaction-scoped; no suspend repository API
     is added.

4. Add Ktor application wiring.
   - Add `javersKtorModule()` and `main()`.
   - Install `installBluetape4kKtorCore()`.
   - Initialize H2/Exposed schema for `CommitTable`, `CdoSnapshotTable`, and
     `OrdersTable`.
   - Create `ExposedCdoSnapshotRepository`, `Javers`, `OrderRepository`, and
     `OrderCommandHandler` explicitly.
   - Avoid introducing DI framework or production auto-configuration.

5. Add Ktor routes and DTOs.
   - Add DTOs with `kotlinx.serialization.Serializable`.
   - Add endpoints:
     - `POST /orders`
     - `POST /orders/{orderId}/paid`
     - `GET /orders/{orderId}`
     - `GET /orders/{orderId}/history`
   - Use route-level validation for non-blank IDs/author/SKU, positive quantity,
     positive unit price, and history limit.
   - Convert unknown order to `404` and invalid requests/state transitions to
     `400`.

6. Add tests.
   - Use Ktor `testApplication`.
   - Use `bluetape4kJsonClient()`, `decodeJsonBody()`, and `shouldHaveStatus()`
     where they fit.
   - Cover create, pay, lookup, history metadata, unknown lookup, invalid
     payload, history cap, `/healthz`, and `/readyz`.
   - Add `junit-platform.properties` and `logback-test.xml`.

7. Update docs and repo registration.
   - Add module `README.md` and `README.ko.md`.
   - Update root `README.md` and `README.ko.md`.
   - Update repo-local `AGENTS.md`.
   - Add CI path filter, test job, status dependency, and artifact.
   - Add Nightly test job, Kover XML command, coverage artifact, coverage
     aggregation `needs`, and nightly status `needs`.

8. Verify locally.
   - `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew :examples-javers-ktor:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
   - `./gradlew :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
   - `rg -n -F "\\'" .github/workflows`
   - `git diff --check`

9. Run Step 6-R 7-Tier code review.
   - Review the new Ktor example module slice.
   - Review root registration/docs/workflow slice.
   - Check Ktor/JDBC blocking boundary docs.
   - Check assertion style, Exposed import style, serializer risk, workflow YAML,
     and CI/Nightly coverage registration.
   - Fix all P0/P1 findings and rerun affected checks.

10. Finish delivery.
    - Add `docs/lessons/2026-06-04-issue-141-ktor-example.md`.
    - Commit with Lore trailers.
    - Push branch.
    - Create PR assigned to `debop` and milestone `0.3.0`.
    - Update PR body after PR review and CI with Step DoD status.
    - Do not merge without explicit user approval.

## Risk Controls

| Risk | Control |
|---|---|
| Ktor accessor mismatch | Compile immediately after dependency edit and adjust central catalog accessors only. |
| Blocking JDBC in Ktor | Keep repository synchronous, document the boundary, and avoid pretending this is high-concurrency production guidance. |
| Serialization failure for rich value types | Prefer simple serializable response DTO fields and verify through Ktor JSON tests. |
| Hidden production API change | Keep all code inside the new example package and avoid modifying production modules. |
| Workflow syntax regression | Run `actionlint` and escaped single quote scan. |
| Assertion style drift | Use bluetape4k assertion/test helpers only. |

## Step 3-R 7-Tier Plan Review

Reviewed scope:

- Plan tasks above
- Spec acceptance criteria
- Step 3-R plan review references
- repo-local module registration rules
- Issue #140 lesson on `examples-javers-*` naming

| Tier | Area | P0 | P1 | P2 | P3 | Evidence |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Validation, 400, and 404 tasks are assigned; no auth/secrets surface is introduced. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Schema initialization, health/readiness, and no background client lifecycle are explicit tasks. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | Production APIs are unchanged; module registration comes before implementation. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Ktor helper reuse, serialization DTOs, and Exposed import rules are explicit. |
| 5 | Tests/Types/Silent Failure | 0 | 0 | 0 | 0 | Each route behavior, error path, health route, and history cap has a named test task. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | JDBC blocking boundary is a documentation and review task; no suspend API is added. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locales, AGENTS, settings, CI, Nightly, Kover, lesson, PR, CI, and DoD are assigned. |

### Iteration Log

- Iteration 1 identified two blocking plan gaps:
  - P1/Tier 5: health/readiness tests were not assigned.
  - P1/Tier 7: Nightly coverage aggregation `needs` was not explicitly assigned.
- Plan edits applied:
  - Added `/healthz` and `/readyz` tests.
  - Added Nightly coverage/status `needs` updates.
- Final gate: `P0 = 0`, `P1 = 0`.
