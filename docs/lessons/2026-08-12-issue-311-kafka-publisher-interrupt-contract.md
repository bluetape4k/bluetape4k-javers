# 교훈 - Kafka publisher는 interrupt 상태와 실패 원인을 보존해야 한다

**관련 이슈**: [#311](https://github.com/bluetape4k/bluetape4k-javers/issues/311)
**영향 모듈**: `examples-javers-exposed-ddd`
**작성일**: 2026-08-12

## 배경

`OrderKafkaEventPublisher`는 동기식 `DomainEventPublisher`로 동작하며
`Producer.send(...).get(30, TimeUnit.SECONDS)`로 broker acknowledgement를
기다린다. 기존 구현은 `Future.get()`에서 발생하는
`InterruptedException`·`TimeoutException`·`ExecutionException`을 구분하지
않았다.

이 구현은 호출자에게 실패를 전달하더라도 `InterruptedException`이 지운
thread interrupt 상태를 복구하지 못한다. 또한 producer failure의 실제 원인이
`ExecutionException` 안에 남고, timeout과 broker failure를 관측 결과만으로
구분하기 어렵다.

## 결정 또는 발견

1. 동기 publisher는 acknowledgement를 최대 30초까지 기다리고, timeout·producer
   failure·interrupt를 각각 설명하는 `RuntimeException`으로 fail-fast 전파한다.
2. `InterruptedException`을 잡을 때는 먼저 `Thread.currentThread().interrupt()`를
   호출해 상위 shutdown/cancellation 흐름이 interrupt 상태를 확인할 수 있게 한다.
3. `ExecutionException`은 원인을 unwrap해 예외 cause로 보존한다. 로그와 오류
   메시지에는 topic만 남기고 order key와 JSON event payload는 기록하지 않는다.
4. 단위 테스트는 MockK future로 세 실패 경로를 고정하고, 실제 Kafka 경계는
   `OrderKafkaEventPublisherIntegrationTest`와 기존 `KafkaServer.Launcher` 기반
   `OrderProjectionFlowTest`로 확인한다. 이미 `bluetape4k-testcontainers`
   fixture가 있으므로 raw `GenericContainer`를 추가하거나 통합검증을 생략하지
   않는다.

## RED/GREEN 검증

새 회귀 테스트를 production 수정 전에 실행했을 때 기존 구현은 다음 세 경로에서
실패했다.

- interrupt: `Expected RuntimeException but got InterruptedException`
- timeout: `Expected RuntimeException but got TimeoutException`
- producer failure: `Expected RuntimeException but got ExecutionException`

수정 후에는 세 테스트가 모두 통과하고, interrupt 경로에서 현재 thread의
interrupt 상태가 다시 설정되며, 예외 메시지에 order key나 payload가 포함되지
않음을 확인한다.

## 검증 결과

- `./gradlew :examples-javers-exposed-ddd:test --tests '*OrderKafkaEventPublisherTest*' --no-build-cache --no-daemon --console=plain` — 3개 단위 회귀 테스트 통과.
- `./gradlew :examples-javers-exposed-ddd:test --no-build-cache --no-daemon --console=plain` — command, projection, domain, publisher 테스트 11개 통과.
- `./gradlew :examples-javers-exposed-ddd:test --tests '*OrderKafkaEventPublisherIntegrationTest*' --no-build-cache --no-daemon --console=plain` — 실제 Kafka broker acknowledgement와 consumer key/payload 검증 1개 통과.
- `./gradlew :examples-javers-exposed-ddd:test --tests '*OrderProjectionFlowTest*' --no-build-cache --no-daemon --console=plain` — `KafkaServer.Launcher`와 `RedisServer.Launcher`를 사용하는 실제 broker·consumer·projection 경로 1개 통과.
- `./gradlew :examples-javers-exposed-ddd:check --no-build-cache --no-daemon --console=plain` — 11개 테스트와 Kover verification 통과.

## 향후 지침

Kafka `Producer`, `KafkaTemplate`, Redis client처럼 외부 인프라 acknowledgement를
기다리는 동기 adapter를 수정할 때는 다음 순서를 따른다.

1. `Future.get` 또는 동등한 wait 지점의 interrupt·timeout·execution failure를
   각각 테스트한다.
2. interrupt를 catch하면 상태를 즉시 복구하고, 원인 예외를 보존한 채 호출자에게
   전파한다.
3. 로그에는 topic 같은 안전한 진단 정보만 남기고 raw key·payload·secret은
   제외한다.
4. 단위 테스트 뒤 `bluetape4k-testcontainers`의 기존 launcher로 실제 broker
   acknowledgement와 consumer key/payload 경로를 순차 검증한다.
5. Docker나 broker를 사용할 수 없으면 통합검증을 성공으로 추정하지 말고
   `PENDING` 또는 `BLOCKED` 근거를 남긴다.
