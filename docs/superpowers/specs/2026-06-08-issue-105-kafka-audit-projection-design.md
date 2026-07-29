# Issue #105 - Kafka Audit Projection 설계

## 목표

Kafka repository의 write-only contract를 보존하면서 JaVers CDO snapshot용 explicit read-side Kafka projection path를 추가한다.

## 근거

- GitHub issue #105는 read-capable Kafka audit projection path를 요구한다.
- #135와 #136은 landing됐다. Kafka repository는 이제 shared event pipeline과 vanilla Kafka publisher를 통해 encoded snapshot payload를 publish한다.
- `KafkaCdoSnapshotRepository`와 `VanillaKafkaCdoSnapshotRepository`는 의도적으로 write-only로 남는다.
- #89는 이미 example level에서 Kafka event plus Redis projection을 보여주지만, 해당 code는 order-domain-specific이므로 reusable audit projection API가 되면 안 된다.
- `javers-persistence-redis`, `javers-exposed`, `javers-core`는 이미 read-capable `CdoSnapshotRepository` implementation을 노출한다. Projection은 또 다른 storage abstraction을 추가하지 않고 이 contract를 재사용해야 한다.
- `bluetape4k-kafka`는 이미 `consumerOf(...)`와 `producerOf(...)` helper를 제공한다. `javers-persistence-kafka`는 이미 이에 의존한다.

## Public API

- `KafkaCdoSnapshotProjectionOptions`를 추가한다.
  - `topic`은 source Kafka topic이며 non-blank여야 한다.
  - `pollTimeout`은 positive여야 한다.
  - `subscribeOnStart` default는 `true`다.
  - `commitOffsetsAfterProjection` default는 `true`다.
  - `skipExistingSnapshots` default는 idempotent replay를 위해 `true`다.
  - `closeConsumerOnClose` default는 caller-owned consumer를 위해 `false`다.
- `KafkaCdoSnapshotProjectionResult`를 추가한다.
  - Polled record, projected snapshot, skipped snapshot을 count한다.
- `KafkaCdoSnapshotProjector`를 추가한다.
  - Kafka `Consumer<String, String>`, JaVers `JsonConverter`, target `CdoSnapshotRepository`, options를 받는다.
  - bluetape4k-kafka `consumerOf(...)`로 repository-owned consumer를 만드는 convenience constructor를 제공한다.
  - Optional consumer ownership에 대해서만 `AutoCloseable`을 구현한다.

## Behavior 계약

- Kafka repository는 write-only로 남는다. 기존 repository read method는 변경하지 않는다.
- Projection은 metadata envelope가 아니라 current Kafka wire value인 encoded JaVers snapshot JSON payload를 decode한다.
- Target repository는 supplied `CdoSnapshotRepository`다. Kafka module 변경 없이 Redis, Exposed, Caffeine 또는 다른 implementation을 사용할 수 있다.
- Projection은 각 polled batch 안에서 deterministic `partition, offset` order로 record를 적용한다.
- Total audit ordering은 source topic이 Kafka 자체가 total order를 제공하도록 configure된 경우에만 deterministic하다. 예를 들면 single-partition topic 또는 각 aggregate를 하나의 partition에 유지하는 partitioning strategy다.
- `skipExistingSnapshots=true`이면 replay는 저장 전에 동일 `globalId`, `commitId`, snapshot version이 target repository에 있는지 확인한다. 이를 통해 이미 projected된 snapshot payload에 대한 rebuild/retry가 idempotent해진다.
- Decode 또는 target save가 실패하면 exception을 propagate한다. Offset은 whole batch projection이 성공한 뒤에만 commit한다.

## 문서

- `javers-persistence-kafka/README.md`와 `README.ko.md`를 갱신한다.
- Write stream용 Kafka repository, materialized read용 read-capable `CdoSnapshotRepository`, replay용 projector로 구성되는 CQRS shape를 보여준다.
- Duplicate handling, offset commit behavior, ordering limit을 문서화한다.

## 테스트 요구사항

- Unit test:
  - Options validation.
  - Convenience constructor가 bluetape4k-kafka consumer helper를 사용하고 repository-owned consumer를 close한다.
  - Idempotent replay가 duplicate snapshot을 skip한다.
  - Decode 또는 save failure가 offset commit을 방지한다.
- Integration test:
  - `VanillaKafkaCdoSnapshotRepository`로 snapshot을 publish한다.
  - `KafkaServer.Launcher`와 `RedisServer.Launcher`를 사용해 Kafka record에서 Redis `LettuceCdoSnapshotRepository` projection을 rebuild한다.
- Targeted Gradle verification:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`.

## Non-goal

- Making Kafka repositories read-capable.
- Composite durable history plus event stream repository (#131).
- Spring Boot auto-configuration (#104).
- New Kafka metadata envelope or header contract.
- Kafka transactions, outbox, or exactly-once processing.
- Ktor example change. Future Ktor example이 이 projection을 consume하면 `bluetape4k-projects` Ktor module을 재사용해야 한다.

## Risk 및 Mitigation

- Risk: Read store로 replay하면 snapshot이 duplicate될 수 있다. Mitigation: `skipExistingSnapshots=true`가 저장 전에 target state를 확인한다.
- Risk: Cross-partition ordering이 total order로 오해될 수 있다. Mitigation: Kafka ordering limit을 문서화하고 deterministic batch order를 적용한다.
- Risk: Partial projection 후 offset이 commit될 수 있다. Mitigation: full batch 성공 후에만 commit한다.
- Risk: Projection이 Kafka를 Redis에 결합할 수 있다. Mitigation: 기존 `CdoSnapshotRepository` interface를 target으로 삼고 Redis는 integration evidence로만 사용한다.
