# Issue #105 - Kafka Audit Projection 7-Tier Review

## Scope

Review target: explicit Kafka snapshot projection API, tests, README updates,
and issue #105 design artifacts.

## Tier 1 - Contract

- P0: 0
- P1: 0
- Kafka repositories remain write-only. No existing read-path method changed.
- New read behavior is isolated in `KafkaCdoSnapshotProjector`.
- Projection target is the existing `CdoSnapshotRepository` contract.

## Tier 2 - Correctness

- P0: 0
- P1: 0
- Decode failures propagate and prevent offset commits.
- Offset commits happen only after a full polled batch succeeds.
- Idempotent replay skips snapshots already present by GlobalId, commit id, and
  version.

## Tier 3 - Ordering And Recovery

- P0: 0
- P1: 0
- Batch application order is deterministic by `partition, offset`.
- README documents that total audit order depends on topic topology.
- `replayUntilIdle` supports bounded rebuild loops without hidden background
  state.

## Tier 4 - Ecosystem Reuse

- P0: 0
- P1: 0
- Uses `bluetape4k-kafka` `consumerOf(...)` for config-based consumers.
- Reuses `JaversCodecs.String` for the current Kafka wire payload.
- Reuses Redis Lettuce and Caffeine `CdoSnapshotRepository` implementations as
  projection targets.
- Reuses `KafkaServer.Launcher` and `RedisServer.Launcher` in integration
  coverage.
- Production `runtimeClasspath` does not include the Redis projection test
  dependencies.

## Tier 5 - API And Documentation

- P0: 0
- P1: 0
- Public data classes are `Serializable` and use companion `invoke` validation.
- KDoc documents behavior contracts and usage.
- `README.md` and `README.ko.md` were updated together.
- `javers-kafka-projection-01.png` is embedded in both README files.
- Matching SVG, DOT, plain, sketch, and Graphviz evidence files are present.

## Tier 6 - Tests

- P0: 0
- P1: 0
- Targeted test evidence:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: `SUCCESS: Executed 39 tests in 10.9s`
- Dependency evidence:
  - `./gradlew -q :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg "javers-persistence-redis|bluetape4k-lettuce|lettuce-core"`
  - Result: no matches.
- Diagram evidence:
  - Rendered `javers-persistence-kafka/docs/images/readme-diagrams/javers-kafka-projection-01.png`.
  - Visually inspected the rendered PNG and fixed projector title overflow before finalizing.
- Coverage includes unit replay behavior and Kafka-to-Redis projection.

## Tier 7 - Risk

- P0: 0
- P1: 0
- Residual risk: replay idempotency is read-store based, not Kafka exactly-once.
- Residual risk: multi-partition total ordering remains a Kafka topic design
  responsibility.
- Residual risk: current wire value has no metadata envelope; #131 or future
  work must define headers/envelope explicitly before relying on wire metadata.

## Gate

PASS. P0=0 and P1=0.
