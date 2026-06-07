# Issue #136 - Vanilla Kafka Snapshot Publisher

## Context

`javers-persistence-kafka` already had a Spring Kafka `KafkaTemplate`-backed
write-only repository. Non-Spring users needed a plain Apache Kafka producer
path, but the reusable `bluetape4k-kafka` helper artifact also includes Spring
Kafka support.

## Decision

Add a `VanillaKafkaCdoSnapshotRepository` that accepts
`Producer<String, String>` directly. Keep `bluetape4k-kafka` / `bluetape4k-kafka4`
as optional producer-creation helpers in README examples, not mandatory runtime
dependencies of `javers-persistence-kafka`.

## Outcome

The module now has two clear adapters:

- `KafkaCdoSnapshotRepository` for Spring Kafka `KafkaTemplate`.
- `VanillaKafkaCdoSnapshotRepository` for Apache Kafka `Producer`.

Producer ownership is explicit through `closeProducerOnClose`, which defaults to
`false`.

## Verification

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`: PASS, 18 tests.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`: production runtime evidence showed `kafka-clients` and no `spring-kafka` / `bluetape4k-kafka` line.
- `git diff --check`: PASS.

## Future Guidance

Keep Kafka repository work write-only until #105 or #131 explicitly owns read
projection or composite durable-history semantics. Do not add helper artifacts
as runtime dependencies when a direct Apache Kafka API keeps the vanilla path
cleaner.

