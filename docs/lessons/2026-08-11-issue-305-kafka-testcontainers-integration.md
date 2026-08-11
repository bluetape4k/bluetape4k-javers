# 교훈 - 인프라 어댑터 변경에서 Testcontainers 통합검증을 생략하지 않는다

**관련 이슈**: [#305](https://github.com/bluetape4k/bluetape4k-javers/issues/305)
**영향 모듈**: `javers-ddd`
**작성일**: 2026-08-11

## 배경

`KafkaDomainEventPublisher`의 fail-fast 계약을 수정하면서 MockK 기반 단위
테스트와 `:javers-ddd:check`만 먼저 실행했다. 단위 테스트는
`KafkaTemplate.send()`가 반환하는 future의 실패·timeout·interrupt와 Spring
transaction 경계를 검증했지만, 실제 broker acknowledgement와 serializer,
consumer 전달 경로는 검증하지 않았다.

이 저장소와 `bluetape4k`에는 이미 `KafkaServer.Launcher` 기반 테스트 인프라가
있었다. 따라서 실제 통합환경을 사용할 수 있는데도 “별도 통합환경 검증”으로
남겨 둔 것은 검증 공백이었다.

## 결정 또는 발견

인프라 클라이언트와 경계를 맞대는 어댑터 변경은 Testcontainers 통합검증을 같은
이슈의 DoD에 포함한다. 단위 테스트가 충분해 보이더라도 다음 조건을 충족해야
한다.

1. `bluetape4k-testcontainers`의 기존 `KafkaServer.Launcher`/서버 헬퍼를
   재사용한다. raw `GenericContainer`를 새로 만들지 않는다.
2. 발행 메서드가 성공했다는 사실만 확인하지 않는다. 실제 broker의
   acknowledgement를 기다린 뒤 consumer에서 resolved key와 payload를 읽어
   확인한다.
3. 토픽과 consumer group은 테스트별로 고유하게 만들고, bounded polling으로
   대기한다. Testcontainers·실제 DB·native 테스트는 모듈 간에도 순차 실행한다.
4. 모듈에 노출된 실제 helper API를 먼저 확인한다. 존재하지 않는 확장 API를
   가정해 통합검증을 포기하지 말고, 현재 의존성 경계에서 지원되는
   `DefaultKafkaProducerFactory`와 `KafkaServer.Launcher` 조합을 사용한다.
5. Docker를 사용할 수 없는 경우에는 성공으로 추정하지 않는다. 정확한 환경
   오류를 `PENDING`/`BLOCKED` 증적으로 남기고, 검증이 실행된 것처럼 보고하지
   않는다.

## 결과

`javers-ddd` 테스트 의존성에 `bt4k.bluetape4k.testcontainers`와
`libs.testcontainers.kafka`를 추가하고,
`KafkaDomainEventPublisherIntegrationTest`를 작성했다. 테스트는
`KafkaServer.Launcher.kafka`에 실제 `KafkaTemplate`을 연결해
`KafkaDomainEventPublisher.publish()`를 호출하고, consumer에서 aggregate id
key와 직렬화된 event payload를 확인한다.

이 검증으로 #305의 DoD에 있던 “실제 Kafka broker/Testcontainers 전송 경로는
별도 검증 대상” 문구를 제거하고, 실제 통합검증 완료로 갱신했다.

## 검증

- `./gradlew :javers-ddd:test --tests 'io.bluetape4k.javers.ddd.kafka.KafkaDomainEventPublisherIntegrationTest' --no-build-cache --no-daemon --console=plain` — Docker Kafka broker 통합 테스트 통과.
- `./gradlew :javers-ddd:check --no-build-cache --no-daemon --console=plain` — 22개 테스트 통과, Kover verification 통과, `BUILD SUCCESSFUL`.
- `git diff --check` 및 `git show --check` — 통과.
- Issue #305 live read-back — milestone `0.4.0`, assignee `debop`, labels `bug`, `test`, `blocker`, `release-prep` 유지; 통합검증 결과 본문·댓글 반영 확인.

## 향후 지침

Kafka, Redis, NATS, database처럼 외부 인프라와 통신하는 생산 어댑터를 수정할
때는 다음 순서를 기본값으로 삼는다.

1. 단위 테스트로 실패 의미와 경계 조건을 잠근다.
2. 기존 `bluetape4k-testcontainers` launcher를 찾아 실제 인프라 통합 테스트를
   추가하거나 실행한다.
3. 통합 결과를 이슈 DoD에 명시하고, 단위 검증과 통합검증을 분리해 보고한다.
4. helper와 Docker가 이미 제공되는데도 통합검증을 생략하는 경우는 정당한
   `N/A`가 아니다. 환경 장애라면 `PENDING`/`BLOCKED`로 남기고 복구 후 다시
   실행한다.
