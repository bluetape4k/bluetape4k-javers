# CLAUDE.md — bluetape4k-javers

Javers 감사(audit)/diff — Redis·Kafka 백엔드 + Exposed 통합.

- **Group**: `io.github.bluetape4k.javers` · **Javers**: 7.11.0 · **bluetape4k**: 1.7.0-SNAPSHOT
- **Publishing**: Maven Central via nmcp

## Module Structure

| Module | Description |
|--------|-------------|
| `javers-core` | Javers extensions, codecs (Fory/Kryo/Protobuf), cache-backed repositories |
| `javers-persistence-kafka` | Kafka-based CDO snapshot persistence |
| `javers-persistence-redis` | Redis (Lettuce/Redisson) CDO snapshot persistence |

## Build Commands

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```
