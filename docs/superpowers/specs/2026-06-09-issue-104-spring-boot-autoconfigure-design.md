# Issue 104 Spring Boot 4 Auto-Configuration Design

## Context

Issue #104 asks for a Spring Boot 4 auto-configuration module for existing
JaVers repository backends. The repository already has Exposed, Redis, Kafka,
and Spring Boot 4 example modules, but no reusable auto-configuration artifact.

## Design

- Add `javers-spring-boot4-autoconfigure` as a library module.
- Keep backend infrastructure application-owned:
  - Exposed `Database`
  - Lettuce `RedisClient`
  - Redisson `RedissonClient`
  - Spring Kafka `KafkaTemplate<String, String>`
  - vanilla Kafka `Producer<String, String>`
- Create a `JaversRepository` only when `bluetape4k.javers.repository.type`
  selects a concrete backend.
- Create a default `Javers` bean only when a repository exists and the
  application has not already provided `Javers`.
- Register each backend as a separate `@AutoConfiguration` class directly in
  `AutoConfiguration.imports` so ordering annotations apply.
- Guard optional backend and codec classes with `@ConditionalOnClass(name = [...])`.
- Expose only safe Redis codec choice `lz4-fory`; do not expose JDK
  serialization in auto-configuration.

## Non-Goals

- Do not create DataSource, Redis, or Kafka client beans.
- Do not replace explicit wiring in `examples/javers-spring-boot4`.
- Do not change repository runtime behavior in existing modules.
- Do not add hard Kover thresholds.

## Acceptance Checks

- `repository.type=none` creates no repository.
- Global disable creates no repository or `Javers`.
- Each supported backend registers its repository when the required bean exists.
- User-provided `JaversRepository` and `Javers` beans are respected.
- Missing optional backend classes do not fail startup.
- `AutoConfiguration.imports` lists all phases directly.
- Root and module README locale sets document dependency, properties, and
  caller-owned infrastructure.
- CI and Nightly include path filters, test jobs, Kover XML artifacts, and
  status/coverage aggregation for the new module.

## 7-Tier Spec Review

| Tier | Result | Notes |
|---|---|---|
| Security | PASS | No secrets or unsafe deserialization exposed; Redis JDK codec is not an option. |
| Ops/SRE | PASS | Startup backs off through conditional beans/classes; infrastructure ownership remains explicit. |
| Structural | PASS | New module depends on existing repository modules through optional boundaries. |
| Kotlin | PASS | Configuration properties are value types; public APIs have English KDoc. |
| Tests | PASS | Slice tests cover backend registration, backoff, and missing classes. |
| Performance | PASS | No hot path or background lifecycle added. |
| Docs/Release | PASS | README locale set, CHANGELOG, AGENTS, CI/Nightly are required deliverables. |

Final gate: `P0 = 0`, `P1 = 0`.
