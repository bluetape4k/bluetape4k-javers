# CLAUDE.md - bluetape4k-javers

JaVers audit/diff integrations with Redis, Kafka, and planned Exposed/DDD layers.

- **Group**: `io.github.bluetape4k.javers`
- **JaVers**: 7.11.0
- **bluetape4k**: `1.7.0-SNAPSHOT`
- **Publishing**: Maven Central through NMCP

## Module Structure

| Module | Description |
|---|---|
| `javers-core` | JaVers extensions, codecs, and cache-backed repositories |
| `javers-persistence-kafka` | Kafka-backed CDO snapshot persistence |
| `javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `bom/` | `bluetape4k-javers-bom` consumer BOM |

## Build Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Rules

- Preserve audit/diff semantics; add regression tests for persistence behavior changes.
- Follow existing codec and cache-backed repository patterns.
- For Redis/Kafka changes, verify the affected persistence module directly.

## Documentation Rules

- Keep `README.md` and `README.ko.md` structurally aligned.
- Store shared README images under `docs/assets/` and reference them with the same relative path from both locales.
- Keep this file and other agent-facing guidance in English.
