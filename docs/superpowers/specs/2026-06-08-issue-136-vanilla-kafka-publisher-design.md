# Issue #136 - Vanilla Kafka Snapshot Publisher Design

## Goal

Add a Spring-free Kafka publish path for JaVers CDO snapshots so non-Spring
applications can publish audit events with the Apache Kafka client API.

## Evidence

- GitHub issue #136: vanilla Kafka snapshot publisher using bluetape4k-kafka.
- Current base: `develop` at `4e49fa7 docs: clarify Redis Exposed latency strategy`.
- Current `KafkaCdoSnapshotRepository` accepts `KafkaTemplate<String, String>`
  and publishes via `sendDefault(...).get(publishTimeout)`.
- `javers-persistence-kafka/build.gradle.kts` exposes `kafka-clients` as `api`
  and keeps `spring-kafka` as `compileOnly`.
- `gradle/libs.versions.toml` contains `bluetape4k-kafka`.
- Sibling `bluetape4k-projects/infra/kafka` provides vanilla `producerOf(...)`
  and `Producer.suspendSend(...)` helpers, but the published Kafka utility
  artifact also contains Spring Kafka support. Therefore `javers-persistence-kafka`
  must not add `bluetape4k-kafka` as a mandatory runtime dependency merely to
  expose a Spring-free repository.
- Prior #40 / `docs/lessons/2026-05-17-javers-0.1.0-prerelease-fixes.md`
  records a P0 class of bug: Kafka publish failures must propagate and
  write-only read behavior must stay explicit.

## Scope

### Public API

- Add `VanillaKafkaCdoSnapshotRepository`.
  - Constructor accepts an Apache Kafka `Producer<String, String>`.
  - Constructor accepts an options object with topic, publish timeout,
    flush-after-send flag, and close-producer-on-close flag.
  - Constructor accepts an optional key mapper with default `snapshot.globalId.value()`.
  - Class implements `AutoCloseable` so opt-in producer ownership is explicit.
- Add `VanillaKafkaCdoSnapshotRepositoryOptions`.
  - `topic` must be non-blank.
  - `publishTimeout` must be positive.
  - `flushAfterSend` defaults to `false`; `send(...).get(...)` already waits
    for acknowledgement.
  - `closeProducerOnClose` defaults to `false` because the caller usually owns
    the Apache Kafka producer lifecycle.

### Behavior Contract

- Publish a `ProducerRecord(topic, key, encodedSnapshot)` for every JaVers
  snapshot persisted.
- Wait up to `publishTimeout` for the Kafka send result.
- Propagate publish failures as `RuntimeException` so
  `AbstractCdoSnapshotRepository.persist()` does not advance the head commit.
- Preserve interrupt status when interrupted.
- Keep the repository write-only, matching the existing Kafka repository:
  read methods return empty/false/0 and log the write-only contract once at warn
  level, then debug.
- Do not make Kafka read-capable in this issue; #105 owns read projection and
  #131 owns composite durable history plus event stream behavior.
- Do not add a mandatory Spring Kafka runtime dependency for the vanilla path.

### Documentation

- Update `javers-persistence-kafka/README.md` and `README.ko.md`.
- Show Spring Kafka and vanilla Kafka adapter choices.
- Show optional `bluetape4k-kafka` usage for producer creation without making it
  a mandatory dependency of `javers-persistence-kafka`.
- Keep the write-only warning visible.

## Test Requirements

- Unit tests:
  - Captures topic, key, and encoded payload for the vanilla repository.
  - Proves publish failure propagation.
  - Proves timeout propagation.
  - Proves interrupted publish restores interrupt status.
  - Proves `flushAfterSend` calls `Producer.flush()` only after successful ack.
  - Proves `closeProducerOnClose=false` does not close the producer.
  - Proves `closeProducerOnClose=true` closes the producer when `close()` is called.
  - Proves blank topic and non-positive timeout validation.
  - Proves write-only read contract warning parity.
- Existing Spring Kafka repository tests must continue to pass.
- Targeted Gradle verification:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Dependency evidence:
  - `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`
  - The production runtime classpath must not contain `spring-kafka`.
- `git diff --check`.

## Non-goals

- Kafka read projection.
- Composite durable history plus event stream repository.
- New module.
- Spring Boot auto-configuration.
- Coroutine repository API.
- Kafka transactions or idempotence defaults beyond documenting that callers
  configure those producer settings.

## Risks and Mitigations

- Risk: Adding `bluetape4k-kafka` as a runtime dependency could reintroduce
  Spring Kafka transitively. Mitigation: accept Apache `Producer` directly and
  document `bluetape4k-kafka` as an optional producer factory/helper.
- Risk: A write-only repository can look read-capable because it implements the
  JaVers repository contract. Mitigation: keep the existing warning behavior
  and README warning.
- Risk: Producer lifecycle ownership is ambiguous. Mitigation:
  `closeProducerOnClose=false` by default and explicit `AutoCloseable` behavior
  when users opt in.
- Risk: Blocking `Future.get()` can hang without a timeout. Mitigation: validate
  positive `publishTimeout` and use it for every send wait.

