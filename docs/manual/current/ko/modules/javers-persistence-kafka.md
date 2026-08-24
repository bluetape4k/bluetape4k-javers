# javers-persistence-kafka

`javers-persistence-kafka`는 하위 projection을 위해 JaVers snapshot event를
Kafka로 발행합니다. 현재 consumer lifecycle은 성공적으로 처리한 비어 있지
않은 batch에서만 offset을 commit하고, 실패한 작업은 재시도할 수 있게 둡니다.

- Gradle project: `:javers-persistence-kafka`
- Artifact: `io.github.bluetape4k.javers:javers-persistence-kafka`
- Source directory: `javers-persistence-kafka`
