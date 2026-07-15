# bluetape4k-javers 0.2 manual

Object auditing becomes difficult when application state, audit history, and query projections are treated as one store. `bluetape4k-javers` 0.2.1 gives Kotlin services a JaVers audit layer with Exposed, Redis, and Kafka adapters, but each adapter has a different responsibility. This manual starts with those boundaries so that a service does not accidentally use a cache or stream as its only recoverable record.

The manual is pinned to release `0.2.1` (`bffe19439ca891fa5301a76421bdef7ba75252a0`). Ktor integration, Spring Boot 4 auto-configuration and examples, and the dedicated Gradle benchmark module were added after that release. They are not 0.2 features.

## Start here

- [Getting started](getting-started.md) installs a coherent ecosystem dependency set and creates a small Exposed-backed JaVers instance.
- [Repository map](architecture/repository-map.md) shows which module owns audit semantics, persistence, and event publication.
- [Persistence selection](persistence/selection-guide.md) compares Exposed, Redis, and Kafka by recovery and query needs.
- [Learning path](guides/learning-path.md) orders the material for application developers, library integrators, and operators.

## Use the manual by question

If you need to understand JaVers data, read [the audit model](architecture/audit-model.md). If you are composing storage, cache, or publication paths, read [repository composition](architecture/repository-composition.md) and [failure contracts](operations/failure-contracts.md). For a command-to-projection example, follow [DDD and CQRS](guides/ddd-and-cqrs.md). For production proof, use [testing](guides/testing.md) and [observability](operations/observability.md).

The release source is the behavior authority. The most important starting points are [`CdoSnapshotRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepository.kt), [`AggregateRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt), and the [`javers-exposed-ddd` example](https://github.com/bluetape4k/bluetape4k-javers/tree/0.2.1/examples/javers-exposed-ddd).

