# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

`bluetape4k-javers` is a Kotlin/JVM library providing Javers auditing and diff capabilities with Redis/Kafka persistence backends and Exposed integration.

## Module Structure

| Module | Description |
|--------|-------------|
| `javers-core` | Javers extensions, codecs (Fory/Kryo/Protobuf), cache-backed repositories |
| `javers-persistence-kafka` | Kafka-based CDO snapshot persistence |
| `javers-persistence-redis` | Redis (Lettuce/Redisson) CDO snapshot persistence |

## Development Guidelines

- **README**: Bilingual `README.md` (English) + `README.ko.md` (Korean)
- **KDoc**: Required on all public classes, interfaces, and extension functions
- **Commits**: Korean + prefix (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`)
- **Kotlin**: 2.3+; no ktlint (IntelliJ IDEA formatter + `.editorconfig`)
- **Tests**: JUnit 5 + MockK + Kluent; Testcontainers for Redis/Kafka

## Build Commands

```bash
./gradlew build -x test        # compile only
./gradlew build                # compile + test
./gradlew :javers-core:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## Build Configuration

- **JVM Toolchain**: Java 21 · **Kotlin**: 2.3 · **Javers**: 7.11.0
- **bluetape4k**: 1.7.0-SNAPSHOT (from Maven Central snapshots)
- **Publishing**: Maven Central via nmcp (`io.github.bluetape4k.javers`)

## Key Design Patterns

- `assertXxx()` → `AssertionError`; `requireXxx()` → `IllegalArgumentException`
- Coroutines-first for async work
- Virtual Threads: never `@Synchronized` — use `reentrantLock()`

## Git Workflow

- Base branch: `develop`
- Commits: Korean + prefix (`feat: ...`, `fix: ...`)
