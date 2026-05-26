# bluetape4k-javers

[![CI](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

English | [한국어](./README.ko.md)

![Bluetape4k JaVers audit workbench](./docs/assets/javers-workbench.png)

Kotlin/JVM integrations for [JaVers](https://javers.org) object auditing and
diffing, with Exposed JDBC, Redis, and Kafka persistence options for CDO snapshots and
event-sourced change streams.

## Project Purpose

`bluetape4k-javers` extends JaVers beyond its built-in in-memory, MongoDB, and
JDBC storage choices. It focuses on Kotlin-friendly audit/diff infrastructure
for services that need cache-backed reads, Redis persistence, Kafka event
streams, and an Exposed-backed repository layer.

## What It Provides

- **JaVers core helpers** — extensions, codecs, and cache-backed repository
  building blocks.
- **Exposed JDBC persistence** — Exposed schema and repository for SQL-backed
  JaVers CDO snapshots.
- **Redis persistence** — Lettuce and Redisson based snapshot storage paths.
- **Kafka persistence** — event-stream backed CDO snapshot persistence.
- **BOM support** — `bluetape4k-javers-bom` for aligned consumer dependency
  versions.
- **Implementation backlog** — documented phase chain for Exposed persistence,
  DDD helpers, and CQRS/Event Sourcing examples.

<!-- README_VISUAL_OVERVIEW:START -->
## Overview Diagram

![Bluetape4k JaVers overview diagram](docs/images/readme-diagrams/root-readme-overview-01.png)

## Module Composition Chart

![Bluetape4k JaVers module composition chart](docs/images/readme-charts/root-readme-module-chart-01.png)
<!-- README_VISUAL_OVERVIEW:END -->

## Architecture

![javers Architecture diagram](docs/images/readme-diagrams/bluetape4k-javers-architecture-01.png)

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| `javers-core` | `io.github.bluetape4k.javers:javers-core` | JaVers extensions, codecs, cache-backed repositories |
| `javers-exposed` | `io.github.bluetape4k.javers:javers-exposed` | Exposed JDBC CDO snapshot persistence |
| `javers-persistence-redis` | `io.github.bluetape4k.javers:javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `javers-persistence-kafka` | `io.github.bluetape4k.javers:javers-persistence-kafka` | Kafka-backed CDO snapshot persistence (write-only event stream; reads always return empty) |
| `bluetape4k-javers-bom` | `io.github.bluetape4k.javers:bluetape4k-javers-bom` | Consumer BOM for aligned JaVers artifacts |

## Requirements

- JDK 21+
- Kotlin 2.3+
- JaVers 7.11.0

## Build

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-exposed:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## References

- [JaVers](https://javers.org)
- [JaVers Feature Overview](https://javers.org/features)
- [JaVers VS Envers Comparison](https://javers.org/blog/2017/12/javers-vs-envers-comparision.html)
- [Using JaVers for Data Model Auditing in Spring Data](https://www.baeldung.com/spring-data-javers-audit)
