# 교훈 - 이슈 176 Spring Kafka 타임아웃 검증

**관련 이슈**: #176
**영향 모듈**: `javers-persistence-kafka`

## L1: 같은 발행 계약은 어댑터 경계마다 같은 시점에 검증한다

### 문제

Vanilla Kafka 경로는 `publishTimeout`을 옵션 생성 시점에 검증했지만,
Spring Kafka 저장소와 발행자 경로는 같은 값을 검증하지 않았다.
그 결과 0 이하의 타임아웃이 생성 경계를 통과해 발행 시점에야
실패하거나 어댑터별로 다르게 동작할 수 있었다.

### 결정

Spring Kafka 저장소와 발행자의 생성 경계에서 동일한 양수
기간 검사 조건을 적용한다. Java `Duration`은 `Comparable`이므로
`bluetape4k-core`의 `requireGt(Duration.ZERO, ...)`를 재사용한다.
발행자는 `init` 검증 대신 비공개 생성자와 companion 객체의 `invoke`에서
검증한 뒤 생성한다.

### 검증

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- 결과: 테스트 24개 통과.

### 다음 작업자를 위한 규칙

Kafka/NATS/SQS 등 스냅샷 이벤트 어댑터에 제한된 대기 옵션이 추가되면,
어댑터별 공개 생성자/옵션과 재사용 가능한 발행자 양쪽에 같은
생성 시점 검증 테스트를 추가한다.
