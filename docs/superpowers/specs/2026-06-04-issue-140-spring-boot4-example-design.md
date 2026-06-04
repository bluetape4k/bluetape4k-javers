# Issue 140 Spring Boot 4 JaVers Example Design

## Context

Issue #140 adds a Spring Boot 4 example application for the current
`bluetape4k-javers` feature set. The repository already contains
`examples/javers-exposed-ddd`, which proves Exposed command persistence, JaVers
snapshot persistence, DDD aggregate helpers, Kafka events, and Redis projection.

This slice adds an HTTP application example without depending on future
auto-configuration or pending Redis/Kafka repository work.

## Goals

- Add `examples/javers-spring-boot4` as a Spring Boot 4 example module.
- Use current `javers-exposed` and `javers-ddd` APIs with explicit wiring.
- Persist order command state through Exposed JDBC and H2.
- Persist JaVers snapshots through `ExposedCdoSnapshotRepository`.
- Provide REST endpoints for command, lookup, and audit-history flows.
- Add focused Spring Boot integration tests.
- Register the module in settings, README locale set, repo module list, CI, and
  Nightly coverage.

## Non-Goals

- No new JaVers repository abstraction.
- No Spring Boot auto-configuration module.
- No Redis/Kafka/NATS/SQS pipeline implementation.
- No production-grade auth, outbox, retry, or deployment guide.
- No change to published production module APIs.

## Module Shape

| Area | Design |
|---|---|
| Module path | `examples/javers-spring-boot4` |
| Gradle project | `:examples-javers-spring-boot4` |
| Package | `io.bluetape4k.javers.examples.springboot4` |
| Runtime | Spring Boot 4, Spring MVC, Exposed JDBC, H2 |
| JaVers storage | `ExposedCdoSnapshotRepository` |
| Aggregate support | `javers-ddd` `AggregateRepository`, `AggregateRoot`, `DomainEvent` |
| Tests | Spring Boot MVC integration tests with `MockMvc` |

## REST Contract

The example exposes small order endpoints. IDs are caller-supplied strings so
tests can assert deterministic history.

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/orders` | Places an order and commits the first JaVers snapshot. |
| `POST` | `/orders/{orderId}/paid` | Marks an existing order as paid and commits the second snapshot. |
| `GET` | `/orders/{orderId}` | Returns the current command-side order state or `404`. |
| `GET` | `/orders/{orderId}/history?limit=20` | Returns newest-first JaVers snapshot metadata and state. |

### Request/Response Rules

- `orderId`, `customerId`, `sku`, and `author` must be non-blank.
- Item quantity must be positive.
- Item unit price must be positive.
- `history.limit` defaults to 20 and is capped at 100.
- Unknown order lookup returns `404`.
- Invalid request payload returns Spring Boot's standard validation error
  response; the example does not add custom exception infrastructure.

## Persistence Design

- `OrdersTable` stores command-side order state.
- `OrderRepository` persists command state inside Exposed transactions and then
  delegates JaVers commits to `AggregateRepository`.
- `JaversConfiguration` creates:
  - H2-backed Exposed `Database`
  - schema initializer for order and JaVers tables
  - `ExposedCdoSnapshotRepository`
  - `Javers`
  - `OrderRepository`
  - `OrderCommandHandler`
- Database setup is example-local and explicit. It does not imply repository-wide
  Spring Boot auto-configuration.

## Test Design

Focused `@SpringBootTest` + `MockMvc` tests must prove:

- `POST /orders` persists command state and creates one JaVers snapshot.
- `POST /orders/{orderId}/paid` updates state and creates a second snapshot.
- `GET /orders/{orderId}` returns the current state.
- `GET /orders/{orderId}/history` returns bounded history with event type
  metadata.
- Unknown order returns `404`.
- Invalid payload returns a client error.

Tests should use bluetape4k assertion extensions in test bodies and avoid
AssertJ/JUnit assertion APIs in new test assertions.

## Documentation and Registration

- Add module-level `README.md` and `README.ko.md`.
- Update root `README.md` and `README.ko.md` module table and test command list.
- Update repo-local `AGENTS.md` module table and command list.
- Register the module in `settings.gradle.kts`.
- Add CI path filter and test job.
- Add Nightly test job and coverage artifact.

## Acceptance Criteria

- `./gradlew :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
  lists `:examples-javers-spring-boot4`.
- `git diff --check` passes.
- Workflow syntax checks pass when tooling is available.
- Spec, plan, and code review each record 7-Tier review with `P0 = 0` and
  `P1 = 0`.

## Alternatives Considered

- Reuse `examples/javers-exposed-ddd` directly as a Spring Boot app: rejected
  because the existing example already owns Kafka/Redis CQRS flow and would make
  the Spring Boot example less focused.
- Add Redis projection endpoints: rejected because Redis read-through/write
  strategies are tracked by separate feature issues and are not required for
  Issue #140.
- Add Spring Boot auto-configuration: rejected because Issue #140 explicitly
  wants an example using current features, not new boot starter behavior.

## Step 2-R 7-Tier Spec Review

Reviewed scope:

- `docs/superpowers/research/2026-06-04-issue-140-spring-boot4-example-research.md`
- `docs/superpowers/specs/2026-06-04-issue-140-spring-boot4-example-design.md`
- Issue #140 requirements
- repo-local `AGENTS.md` module registration rules

| Tier | Area | P0 | P1 | P2 | P3 | Evidence |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | Example has no auth boundary and no secrets; validation rules cover caller-controlled identifiers and numeric fields. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Explicit example-local H2/Exposed startup and no background clients; production retry/outbox is non-goal. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | New example module only; no production public API change. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Kotlin data contracts and explicit package/module boundaries are specified; public KDoc impact is example-local. |
| 5 | Testability/Types/Silent Failure | 0 | 0 | 0 | 0 | Success, failure, lookup, and bounded-history tests are named. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | History limit default and cap are specified; no unbounded Redis/Kafka/Testcontainers loop. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locale set, AGENTS, CI, Nightly, coverage, Gradle projects checks are required. |

### Iteration Log

- Iteration 1 identified two blocking spec gaps before finalizing:
  - P1/Tier 5: history endpoint did not specify bounded result semantics.
  - P1/Tier 7: CI/Nightly coverage artifact registration was not explicit.
- Spec edits applied:
  - Added `history.limit` default/cap.
  - Added CI/Nightly coverage artifact registration to documentation and
    acceptance criteria.
- Final gate: `P0 = 0`, `P1 = 0`.
