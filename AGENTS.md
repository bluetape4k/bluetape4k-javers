# AGENTS.md - bluetape4k-javers

This repository inherits the workspace guidance from `../AGENTS.md`.
Read and follow the workspace root guide first. This file only adds
repo-specific layout, commands, domain rules, and local exceptions.


Javers audit/diff integrations with Redis, Kafka, and Exposed.

- Group: `io.github.bluetape4k.javers`
- Javers: 7.11.0
- bluetape4k: `1.7.0-SNAPSHOT`
- Publishing: Maven Central through NMCP

## Modules

| Module | Purpose |
|---|---|
| `javers-core` | Javers extensions, codecs, cache-backed repositories |
| `javers-ddd` | DDD aggregate and domain-event helpers for JaVers workflows |
| `javers-exposed` | Exposed JDBC CDO snapshot persistence |
| `benchmark/javers-exposed-benchmark` | kotlinx-benchmark harness for JaVers Exposed PostgreSQL index tradeoff evaluation |
| `examples/javers-exposed-ddd` | CQRS command-side example using Exposed persistence and JaVers DDD helpers |
| `examples/javers-ktor` | Ktor REST example using explicit Exposed and JaVers wiring |
| `examples/javers-spring-boot4` | Spring Boot 4 REST example using explicit Exposed and JaVers wiring |
| `javers-persistence-kafka` | Kafka-backed CDO snapshot persistence |
| `javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `bom/` | `bluetape4k-javers-bom` consumer BOM |

## Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-ddd:test
./gradlew :javers-exposed:test
./gradlew :examples-javers-exposed-ddd:test
./gradlew :examples-javers-ktor:test
./gradlew :examples-javers-spring-boot4:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Rules

- Preserve audit/diff semantics; add regression tests for persistence behavior
  changes.
- Follow existing codec and cache-backed repository patterns.
- For Redis/Kafka changes, verify the affected persistence module directly.

## Repo-Specific Guards

- Preserve Redis/Kafka persistence semantics and verify the affected persistence
  module directly.
- Run Redis/Kafka/Testcontainers-backed verification sequentially.
