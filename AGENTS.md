# AGENTS.md - bluetape4k-javers

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
| `examples/javers-exposed-ddd` | CQRS command-side example using Exposed persistence and JaVers DDD helpers |
| `examples/javers-spring-boot4` | Spring Boot 4 REST example using explicit Exposed and JaVers wiring |
| `javers-persistence-kafka` | Kafka-backed CDO snapshot persistence |
| `javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `bom/` | `bluetape4k-javers-bom` consumer BOM |

Root README visual assets live under `docs/assets/` and should be shared by
`README.md` and `README.ko.md` through the same relative path.

## Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-ddd:test
./gradlew :javers-exposed:test
./gradlew :examples-javers-exposed-ddd:test
./gradlew :examples-javers-spring-boot4:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Rules

- Preserve audit/diff semantics; add regression tests for persistence behavior
  changes.
- Follow existing codec and cache-backed repository patterns.
- For Redis/Kafka changes, verify the affected persistence module directly.

## Cross-Repo Lesson Guards

- Before issue, PR, workflow, release, or module-registration work, query GNO
  for this repo in both `bluetape4k-github` and `bluetape4k-docs`.
- For module additions, moves, or artifact renames, update README locale sets,
  repo-local module lists, CI/Nightly coverage, coverage artifacts, and
  BOM/catalog constraints together.
- Run Redis/Kafka/Testcontainers-backed verification sequentially. Preserve
  Kover XML/Codecov visibility without adding hard gates unless explicitly
  decided.
