# javers-persistence-kafka

`javers-persistence-kafka` publishes JaVers snapshot events to Kafka for
downstream projections. The current consumer lifecycle commits offsets only for
successfully handled, non-empty batches and leaves failed work retryable.

- Gradle project: `:javers-persistence-kafka`
- Artifact: `io.github.bluetape4k.javers:javers-persistence-kafka`
- Source directory: `javers-persistence-kafka`
