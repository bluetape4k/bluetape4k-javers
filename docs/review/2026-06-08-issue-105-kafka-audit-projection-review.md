# Issue #105 - Kafka Audit Projection 7-Tier 검토

## 범위

검토 target은 explicit Kafka snapshot projection API, tests, README updates,
issue #105 design artifact다.

## Tier 1 - 계약 검토

- P0: 0
- P1: 0
- Kafka repository는 write-only로 유지된다. 기존 read-path method는 변경하지 않았다.
- 새 read behavior는 `KafkaCdoSnapshotProjector`에 격리된다.
- Projection target은 기존 `CdoSnapshotRepository` contract다.

## Tier 2 - 정확성

- P0: 0
- P1: 0
- Decode failure는 propagate되며 offset commit을 막는다.
- Offset commit은 전체 polled batch가 성공한 뒤에만 수행된다.
- Idempotent replay는 GlobalId, commit id, version 기준으로 이미 존재하는
  snapshot을 건너뛴다.

## Tier 3 - Ordering 및 Recovery

- P0: 0
- P1: 0
- Batch application order는 `partition, offset` 기준으로 deterministic하다.
- README는 total audit order가 topic topology에 의존함을 문서화한다.
- `replayUntilIdle`은 hidden background state 없이 bounded rebuild loop를 지원한다.

## Tier 4 - Ecosystem 재사용

- P0: 0
- P1: 0
- Config-based consumer에는 `bluetape4k-kafka` `consumerOf(...)`를 사용한다.
- 현재 Kafka wire payload에는 `JaversCodecs.String`을 재사용한다.
- Projection target으로 Redis Lettuce와 Caffeine `CdoSnapshotRepository`
  implementation을 재사용한다.
- Integration coverage에서는 `KafkaServer.Launcher`와 `RedisServer.Launcher`를
  재사용한다.
- Production `runtimeClasspath`에는 Redis projection test dependency가 포함되지 않는다.

## Tier 5 - API 및 Documentation

- P0: 0
- P1: 0
- Public data class는 `Serializable`이며 companion `invoke` validation을 사용한다.
- KDoc은 behavior contract와 usage를 문서화한다.
- `README.md`와 `README.ko.md`를 함께 갱신했다.
- `javers-kafka-projection-01.png`는 두 README 파일에 모두 embedded되어 있다.
- 매칭되는 SVG, DOT, plain, sketch, Graphviz evidence file이 존재한다.

## Tier 6 - Test

- P0: 0
- P1: 0
- Targeted test evidence:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: `SUCCESS: Executed 39 tests in 10.9s`
- Dependency evidence:
  - `./gradlew -q :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain | rg "javers-persistence-redis|bluetape4k-lettuce|lettuce-core"`
  - Result: no matches.
- Diagram evidence:
  - Rendered `javers-persistence-kafka/docs/images/readme-diagrams/javers-kafka-projection-01.png`.
  - rendered PNG를 visual inspection했고, finalizing 전에 projector title overflow를 수정했다.
- Coverage는 unit replay behavior와 Kafka-to-Redis projection을 포함한다.

## Tier 7 - Risk

- P0: 0
- P1: 0
- 잔여 위험: replay idempotency는 read-store 기반이며 Kafka exactly-once가 아니다.
- 잔여 위험: multi-partition total ordering은 Kafka topic design responsibility로
  남는다.
- 잔여 위험: 현재 wire value에는 metadata envelope가 없다. wire metadata에 의존하기
  전에 #131 또는 future work에서 headers/envelope를 명시적으로 정의해야 한다.

## Gate

PASS. P0=0 및 P1=0.
