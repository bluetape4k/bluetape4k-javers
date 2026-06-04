# Issue 4 javers-ddd Plan

## Work Type

Type A Full Design: new Gradle module, public API, optional integration
adapters, tests, multilingual README, CI/Nightly wiring, WIP update, and PR.

## Scope

Touch:

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `javers-ddd/**`
- `README.md`, `README.ko.md`
- `bom/README.md`, `bom/README.ko.md`, `bom/build.gradle.kts`
- `.github/workflows/ci.yml`
- `.github/workflows/nightly-tests.yml`
- `WIP.md`
- `docs/lessons/2026-05-26-issue-4-javers-ddd.md`

## Steps

1. Register `javers-ddd` module.
2. Add module dependencies:
   - `api(project(":javers-core"))`
   - `testImplementation(project(":javers-exposed"))` to verify Phase 2
     integration without forcing Exposed as a transitive runtime dependency of
     all DDD helper consumers.
   - `compileOnly(libs.spring.kafka)` for Spring/Kafka adapter APIs.
   - `compileOnly(libs.bluetape4k.nats)` for NATS adapter API.
   - test dependencies for H2, MockK, bluetape4k assertions, and Exposed.
3. Implement public API:
   - `AggregateRoot`
   - `DomainEvent`
   - `toJaversProperties`
   - `DomainEventPublisher`
   - `NoopDomainEventPublisher`
   - `FunctionDomainEventPublisher`
   - `CompositeDomainEventPublisher`
   - `AggregateRepository`
   - Spring/Kafka/NATS publishers.
4. Add tests:
   - event property mapping,
   - publisher dispatch behavior,
   - aggregate save/load/history with H2 and `ExposedCdoSnapshotRepository`.
5. Add README and localized README with Mermaid class diagrams and usage.
6. Update root README and BOM docs.
7. Wire CI/Nightly path filters, jobs, coverage artifact, and status needs.
8. Update `WIP.md` to mark #4 as completed/current and leave #5 as next.
9. Add lesson entry.
10. Verify:
    - `./gradlew :javers-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `./gradlew :javers-ddd:cleanTest :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `actionlint`
    - `git diff --check`
11. Run local/native 7-tier review and require P0/P1=0 before PR creation.
12. Commit with Lore trailers, push, and open a PR to `develop` closing #4.

## Stop Condition

Stop when PR is open with `Closes #4`, validation evidence is recorded in the
PR body, and the local branch has no unstaged changes.

## Known Tradeoffs

- The issue's sealed `DomainEvent` sketch is intentionally changed to an
  interface because library consumers must define event types outside this
  module.
- Publisher adapters are immediate delivery helpers, not a durable outbox.
