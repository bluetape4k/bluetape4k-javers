# Issue 141 Ktor JaVers Example Design

## Context

Issue #141 adds a Ktor example application for the current `bluetape4k-javers`
feature set. Issue #140 already added the Spring Boot 4 counterpart and fixed
the repository convention that example Gradle project names should start with
`:examples-javers-*`.

This slice must prove non-Spring users can wire Exposed command persistence,
`ExposedCdoSnapshotRepository`, JaVers commits, and `javers-ddd` aggregate
helpers in a Ktor application without depending on pending Redis/Kafka/pipeline
features.

## Goals

- Add `examples/javers-ktor` as a Ktor example module.
- Register it as `:examples-javers-ktor`.
- Use current `javers-exposed` and `javers-ddd` APIs with explicit wiring.
- Persist order command state through Exposed JDBC and H2.
- Persist JaVers snapshots through `ExposedCdoSnapshotRepository`.
- Provide Ktor routes for command, lookup, and audit-history flows.
- Reuse `bluetape4k-ktor-core` and `bluetape4k-ktor-testing` where they fit.
- Add focused Ktor `testApplication` integration tests.
- Register the module in settings, README locale set, repo module list, CI, and
  Nightly coverage.

## Non-Goals

- No new JaVers repository abstraction.
- No Redis projection endpoint.
- No Kafka/NATS/SQS pipeline or vanilla Kafka adapter.
- No Spring Boot auto-configuration.
- No production-grade auth, outbox, retry, metrics, or deployment guide.
- No production module public API change.

## Module Shape

| Area | Design |
|---|---|
| Module path | `examples/javers-ktor` |
| Gradle project | `:examples-javers-ktor` |
| Package | `io.bluetape4k.javers.examples.ktor` |
| Runtime | Ktor 3, CIO, Exposed JDBC, H2 |
| Ktor helpers | `bluetape4k-ktor-core`, `bluetape4k-ktor-testing` |
| JaVers storage | `ExposedCdoSnapshotRepository` |
| Aggregate support | `javers-ddd` `AggregateRepository`, `AggregateRoot`, `DomainEvent` |
| Tests | Ktor `testApplication` with JSON client |

## Route Contract

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/orders` | Places an order and commits the first JaVers snapshot. |
| `POST` | `/orders/{orderId}/paid` | Marks an existing order as paid and commits the second snapshot. |
| `GET` | `/orders/{orderId}` | Returns current command-side order state or `404`. |
| `GET` | `/orders/{orderId}/history?limit=20` | Returns newest-first JaVers snapshot metadata and state. |

### Request/Response Rules

- `orderId`, `customerId`, `sku`, and `author` must be non-blank.
- Item quantity must be positive.
- Item unit price must be positive.
- `history.limit` defaults to 20 and is capped at 100.
- Unknown order lookup and history lookup return `404`.
- Invalid request payload or illegal state transition returns `400` through
  bluetape4k Ktor core error responses.
- Response DTOs avoid serializer-heavy value types where practical; monetary and
  timestamp values may be exposed as strings if that keeps the example focused
  on JaVers rather than custom JSON serializers.

## Persistence and Wiring Design

- `OrdersTable` stores command-side order state.
- `OrderRepository` persists command state in Exposed transactions and delegates
  JaVers commits to `AggregateRepository`.
- `javersKtorModule()` creates:
  - H2-backed Exposed `Database`
  - schema initialization for order and JaVers tables
  - `ExposedCdoSnapshotRepository`
  - `Javers`
  - `OrderRepository`
  - `OrderCommandHandler`
  - Ktor route registration
- The default module is self-contained for examples and tests. It does not
  introduce a dependency injection framework.

## Ktor/JDBC Blocking Boundary

The example intentionally uses synchronous Exposed JDBC because the current
JaVers Exposed repository is JDBC-backed. It is acceptable for an example and
fits virtual-thread-friendly JVM runtime experiments, but it is not presented as
a production event-loop blocking recommendation. README must state that
production Ktor deployments should consider worker dispatcher isolation,
virtual-thread execution, or a future R2DBC path before using the same shape at
high concurrency.

## Test Design

Focused Ktor integration tests must prove:

- `POST /orders` persists command state and creates one JaVers snapshot.
- `POST /orders/{orderId}/paid` updates state and creates a second snapshot.
- `GET /orders/{orderId}` returns current state.
- `GET /orders/{orderId}/history` returns bounded history with domain event type
  metadata.
- Unknown order lookup returns `404`.
- Invalid create payload returns `400`.
- Invalid history limit is capped to 100.
- `/healthz` and `/readyz` from `bluetape4k-ktor-core` are available.

Tests must use bluetape4k assertion extensions and Ktor test helpers instead of
JUnit/AssertJ assertions.

## Documentation and Registration

- Add module-level `README.md` and `README.ko.md`.
- Update root `README.md` and `README.ko.md`.
- Update repo-local `AGENTS.md`.
- Register the module in `settings.gradle.kts`.
- Add CI path filter and test job.
- Add Nightly test job, coverage command, coverage artifact, and aggregation
  dependencies.

## Acceptance Criteria

- `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `./gradlew :examples-javers-ktor:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
  lists `:examples-javers-ktor`.
- `./gradlew :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
  passes when available.
- `rg -n -F "\\'" .github/workflows` returns no matches.
- `git diff --check` passes.
- Spec, plan, and code review each record 7-Tier review with `P0 = 0` and
  `P1 = 0`.

## Alternatives Considered

- Reuse the Spring Boot 4 controller directly: rejected because Issue #141 is
  meant to demonstrate non-Spring Ktor wiring.
- Add Redis projection or Kafka publishing endpoints: rejected because these
  are tracked by separate future feature issues and would blur acceptance.
- Add a shared example-domain module: rejected for this slice because it would
  introduce another module and dependency edge only to reduce example-local
  duplication.
- Use custom JSON serializers for the full domain DTO shape: rejected unless
  compile/test requires it; string response fields are enough to prove audit
  behavior.

## Step 2-R 7-Tier Spec Review

Reviewed scope:

- `docs/superpowers/research/2026-06-04-issue-141-ktor-example-research.md`
- `docs/superpowers/specs/2026-06-04-issue-141-ktor-example-design.md`
- Issue #141 requirements
- Ktor official documentation evidence from Context7
- `bluetape4k-projects` Ktor helper examples
- repo-local `AGENTS.md` module registration rules

| Tier | Area | P0 | P1 | P2 | P3 | Evidence |
|---|---|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | No auth/secrets boundary is added; caller input validation and 400/404 contracts are specified. |
| 2 | Ops/SRE Reliability | 0 | 0 | 0 | 0 | Example-local H2/Exposed startup is explicit; `bluetape4k-ktor-core` health/readiness routes are required. |
| 3 | Structural Impact | 0 | 0 | 0 | 0 | New example module only; production module APIs remain unchanged. |
| 4 | Kotlin/API Quality | 0 | 0 | 0 | 0 | Ktor helper reuse and example-local package boundaries are specified; no new shared abstraction. |
| 5 | Testability/Types/Silent Failure | 0 | 0 | 0 | 0 | Success, failure, lookup, history, cap, and health tests are named. |
| 6 | Performance/Stability | 0 | 0 | 0 | 0 | JDBC blocking boundary is explicit and must be documented; history limit is capped. |
| 7 | Docs/Release/Evidence | 0 | 0 | 0 | 0 | README locale set, AGENTS, settings, CI, Nightly, Kover, and workflow checks are required. |

### Iteration Log

- Iteration 1 identified three blocking spec gaps before finalizing:
  - P1/Tier 2: health/readiness route expectation was not explicit.
  - P1/Tier 5: invalid payload and history cap tests were not named.
  - P1/Tier 6: Ktor event-loop blocking risk was not documented.
- Spec edits applied:
  - Added `/healthz` and `/readyz` acceptance.
  - Added invalid payload and history cap test requirements.
  - Added the Ktor/JDBC blocking boundary section.
- Final gate: `P0 = 0`, `P1 = 0`.
