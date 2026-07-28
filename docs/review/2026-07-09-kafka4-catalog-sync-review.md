# Kafka4 catalog sync review

## 범위

- `gradle/libs.versions.toml`
- Kafka-consuming modules:
  - `:javers-persistence-kafka`
  - `:javers-spring-boot4-autoconfigure`
  - `:javers-ddd`
  - `:examples-javers-exposed-ddd`

## 결과

- P0/P1 findings: 0
- 변경은 `bluetape4k-dependencies` source-of-truth value와 일치한다.
- `dependencyInsight`는 `org.apache.kafka:kafka-clients`를 `4.2.1`로 resolve한다.

## 검증

- `sync-shared-versions.py --workspace /tmp/bt4k-kafka4-sync-workspace --check --summary`: `Shared versions are aligned.`
- `./gradlew :javers-persistence-kafka:dependencyInsight --configuration testRuntimeClasspath --dependency org.apache.kafka:kafka-clients --no-daemon --no-configuration-cache --no-build-cache`: `BUILD SUCCESSFUL in 17s`
- `./gradlew :javers-persistence-kafka:test :javers-spring-boot4-autoconfigure:test :javers-ddd:test :examples-javers-exposed-ddd:test --max-workers=1 --no-daemon --no-configuration-cache --no-build-cache`: `BUILD SUCCESSFUL in 1m 1s`
- `git diff --check`
