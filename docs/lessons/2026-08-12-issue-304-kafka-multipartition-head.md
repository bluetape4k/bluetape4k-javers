# Issue #304 Kafka 다중 파티션 replay에서 전역 head 보호

## Context

`KafkaCdoSnapshotProjector`는 현재 Kafka record value에 들어 있는 인코딩된
JaVers snapshot만 읽는다. source repository sequence가 wire payload에 없기
때문에 여러 partition의 record를 poll 순서로 `projectSnapshot`하면 대상
저장소가 부여하는 sequence와 전역 JaVers head가 source 순서와 달라질 수 있다.

## Decision or Finding

- wire-visible monotonic sequence와 migration 계약이 정의되기 전에는 snapshot
  topic을 정확히 하나의 partition으로 제한한다.
- projector는 첫 `poll` 전에 `consumer.partitionsFor(topic)`를 확인하고,
  partition 수가 1이 아니면 `IllegalStateException`으로 fail-closed한다.
- 성공한 topology 검사는 projector 수명 동안 재사용한다. 검증에 실패하면
  성공 상태로 기록하지 않아 다음 실행이 다시 검증할 수 있다.
- 다중 partition 지원은 target-local poll 순서에 기대지 않고 source sequence를
  운반하는 별도 wire 계약과 함께 설계한다.

## Outcome

다중 partition fixture는 decode, projection, offset commit보다 먼저 차단되고
대상 저장소의 head가 비어 있는 상태를 유지한다. 단일 partition replay는 기존
`partition, offset` 순서, 중복 skip, 성공 후 offset commit 계약을 유지한다.

## Verification

- Mock consumer 회귀 테스트로 다중 partition fail-closed와 topology 검증 1회
  계약을 확인했다.
- `KafkaCdoSnapshotProjectorIntegrationTest`는 `bluetape4k-testcontainers`가
  제공하는 실제 Kafka broker에서 write-only snapshot stream을 Redis projection으로
  재생하고 head 및 snapshot 순서를 검증한다.
- 변경된 모듈의 Gradle test/check와 관련 Exposed/Redis 검증은 모두 순차 실행한다.

## Future Guidance

다중 partition projector를 다시 허용하려면 source repository sequence를
wire-visible envelope 또는 header로 정의하고, 기존 payload와의 호환·재생·중복
제거 정책을 먼저 문서화한다. Kafka topology를 mock만으로 검증하지 말고
`bluetape4k-testcontainers` 기반 실제 broker 테스트를 함께 유지한다.
