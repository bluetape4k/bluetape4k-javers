# Issue 104 Spring Boot Auto-Configuration

## Context

Spring Boot auto-configuration for JaVers repositories crossed code, docs,
module registration, and workflow coverage.

## Decision

Keep all infrastructure clients application-owned and make each repository
backend a directly registered auto-configuration phase with class-name guards.

## Outcome

The new module registers Exposed, Lettuce, Redisson, Spring Kafka, and vanilla
Kafka repository backends only when explicitly selected and when the required
bean/class boundary exists.

Post-PR independent review found that Spring Kafka auto-configuration needed to
run after Spring Boot 4 Kafka auto-configuration, that `kafka.topic` had to be
applied to the Spring Kafka repository path, and that Exposed schema creation
should be opt-in by default.
The same review also caught public API compatibility risk when adding topic
support to existing Kafka repository/publisher constructors.

## Verification

- `./gradlew :javers-spring-boot4-autoconfigure:test ...` passed with 17 tests
  after adding Boot-created `KafkaTemplate`, configured topic, and DDL default
  coverage.
- `./gradlew :javers-persistence-kafka:test ...` passed with 41 tests after
  adding explicit-topic publisher coverage.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` passed.
- `./gradlew build -x test ...` passed.

## Future Guard

For new Spring Boot auto-configuration modules, update `AutoConfiguration.imports`,
README locale files, CHANGELOG, `AGENTS.md`, CI, Nightly, and Kover artifacts in
the same PR. Guard compile-only bean signatures with `@ConditionalOnClass(name = [...])`.
When an independent 7-Tier lane needs a subagent but the callable tool lacks an
OMX `agent_type` field, use bounded role-injected subagent prompts or OMX team
runtime instead of skipping the lane. For Boot 4 integrations, verify modular
auto-configuration class names and ordering against the real Boot module.
When extending public Kotlin constructors or companion factories, preserve the
existing parameter order and JVM descriptors; add explicit overloads or named
factories for new options, then verify with compile tests or `javap`.
