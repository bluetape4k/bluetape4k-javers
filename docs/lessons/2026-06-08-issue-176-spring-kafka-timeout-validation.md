# Lessons Learned - Issue 176 Spring Kafka Timeout Validation

**관련 이슈**: #176
**영향 모듈**: `javers-persistence-kafka`

## L1: 같은 publish contract는 adapter 경계마다 같은 시점에 검증한다

### 문제

Vanilla Kafka 경로는 `publishTimeout`을 options 생성 시점에 검증했지만,
Spring Kafka repository/publisher 경로는 같은 값을 검증하지 않았다.
그 결과 zero/negative timeout이 construction boundary를 통과해 publish 시점에야
실패하거나 adapter별로 다르게 동작할 수 있었다.

### 결정

Spring Kafka repository와 publisher construction boundary에서 동일한 positive
duration guard를 적용한다. Java `Duration`은 `Comparable`이므로
`bluetape4k-core`의 `requireGt(Duration.ZERO, ...)`를 재사용한다.
publisher는 `init` 검증 대신 private constructor와 companion `invoke`에서
검증한 뒤 생성한다.

### 검증

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Result: 24 tests passed.

### 다음 작업자를 위한 규칙

Kafka/NATS/SQS 등 snapshot event adapter에 bounded wait option이 추가되면,
adapter-specific public constructor/options와 reusable publisher 양쪽에 같은
construction-time validation test를 추가한다.
