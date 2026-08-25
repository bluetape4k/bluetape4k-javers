# bluetape4k-javers 1.0.0 current manual

This is the current bilingual manual surface for the `1.0.0` release line. It
is generated from the modules registered in `settings.gradle.kts`, so examples
and the benchmark remain visible without being mistaken for published Maven
artifacts.

The release-pinned `0.3.0` manual remains immutable under the original
`docs/manual/en` and `docs/manual/ko` surfaces. This current surface documents
the post-0.3 modules and contracts from the source tree used by the release
workflow.

## Module map

- Foundation: [javers-core](modules/javers-core.md), [javers-ddd](modules/javers-ddd.md), and [the consumer BOM](modules/bluetape4k-javers-bom.md)
- Persistence: [javers-exposed](modules/javers-exposed.md), [javers-persistence-redis](modules/javers-persistence-redis.md), and [javers-persistence-kafka](modules/javers-persistence-kafka.md)
- Framework integration: [Spring Boot 4 auto-configuration](modules/javers-spring-boot4-autoconfigure.md)
- Runnable examples: [Exposed DDD](modules/examples-javers-exposed-ddd.md), [Ktor](modules/examples-javers-ktor.md), and [Spring Boot 4](modules/examples-javers-spring-boot4.md)
- Evidence: [the Exposed benchmark](modules/benchmark-javers-exposed-benchmark.md)

Published coordinates are listed only for library and BOM modules. Examples
and benchmark modules are source-tree consumers and are intentionally not
published as Maven artifacts.
