# Kafka4 catalog sync review

## Scope

- `gradle/libs.versions.toml`
- Kafka-consuming modules:
  - `:javers-persistence-kafka`
  - `:javers-spring-boot4-autoconfigure`
  - `:javers-ddd`
  - `:examples-javers-exposed-ddd`

## Findings

- P0/P1 findings: 0
- The change matches the `bluetape4k-dependencies` source-of-truth value.
- `dependencyInsight` resolves `org.apache.kafka:kafka-clients` to `4.2.1`.

## Verification

- `sync-shared-versions.py --workspace /tmp/bt4k-kafka4-sync-workspace --check --summary`: `Shared versions are aligned.`
- `./gradlew :javers-persistence-kafka:dependencyInsight --configuration testRuntimeClasspath --dependency org.apache.kafka:kafka-clients --no-daemon --no-configuration-cache --no-build-cache`: `BUILD SUCCESSFUL in 17s`
- `./gradlew :javers-persistence-kafka:test :javers-spring-boot4-autoconfigure:test :javers-ddd:test :examples-javers-exposed-ddd:test --max-workers=1 --no-daemon --no-configuration-cache --no-build-cache`: `BUILD SUCCESSFUL in 1m 1s`
- `git diff --check`
