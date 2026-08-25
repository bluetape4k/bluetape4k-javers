# examples-javers-exposed-ddd

This runnable example connects Exposed command-side persistence, JaVers DDD
helpers, Kafka event publication, and a Redis projection. The application store
remains the source of truth while the audit and projection paths keep their
explicit responsibilities.

- Gradle project: `:examples-javers-exposed-ddd`
- Source directory: `examples/javers-exposed-ddd`
- Publication: none
