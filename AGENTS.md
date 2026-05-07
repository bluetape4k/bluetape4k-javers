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
| `javers-persistence-kafka` | Kafka-backed CDO snapshot persistence |
| `javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |

## Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Rules

- Preserve audit/diff semantics; add regression tests for persistence behavior
  changes.
- Follow existing codec and cache-backed repository patterns.
- For Redis/Kafka changes, verify the affected persistence module directly.
