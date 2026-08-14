# 이슈 #328 Slf4jDispatcher 운영 로그 payload 비노출

**관련 이슈**: [#328](https://github.com/bluetape4k/bluetape4k-javers/issues/328)
**선행 작업**: [#313](https://github.com/bluetape4k/bluetape4k-javers/issues/313)
**영향 모듈**: `javers-core`

## 배경

`Slf4jDispatcher`는 `sendSaved`와 `sendDeleted`에서 domain object의
`toString()` 결과를, `sendDeletedById`에서 raw identifier와 `Class` 표현을
INFO 로그에 직접 삽입하고 있었다. 운영 SLF4J logger가 파일·수집기로 연결되면
객체 payload, token, 개인정보, 내부 식별자가 로그에 남을 수 있다.

`ConsoleDispatcher`에는 타입 요약 정책이 적용됐지만, 별도 운영 adapter인
`Slf4jDispatcher`에는 같은 경계가 적용되지 않은 것이 이번 이슈에서 확인한
누락이다.

## 결정 또는 발견

- 세 이벤트 모두 이벤트 종류와 안전한 타입 요약만 INFO에 기록한다.
- `sendSaved`와 `sendDeleted`는 객체의 Kotlin `simpleName`만 기록한다.
- `sendDeletedById`는 `domainType`만 기록하고 `domainObjectId`는 의도적으로
  기록하지 않는다.
- `simpleName`을 확인할 수 없는 anonymous type은 고정 문자열 `<anonymous>`로
  대체해 `toString()`이나 qualified name에 의존하지 않는다.
- `JaversDispatcher` 공개 계약과 dispatcher wiring은 변경하지 않고, 운영
  로깅 정책을 `Slf4jDispatcher` 내부 구현과 KDoc에 한정한다.

## 결과

운영 SLF4J 로그에는 `type=...` 요약만 남고 payload와 identifier는 남지 않는다.
실제 logger backend를 사용하는 `ListAppender` 회귀 테스트가 저장·삭제·ID 삭제
세 경로의 메시지를 캡처해 타입 요약과 비노출을 동시에 확인한다.

## 검증

- RED: `slf4j dispatcher does not expose payload or identifier values` 회귀를
  production 수정 전에 실행했다. 기존 raw `toString()` 로그가
  `Send saved domain object. SensitivePayload(token=secret-token)`를 기록해
  타입 요약 assertion에서 실패했다.
- GREEN: `./gradlew :javers-core:test --tests
  'io.bluetape4k.javers.dispatcher.JaversDispatcherTest' --no-build-cache
  --no-daemon --console=plain` — 6개 테스트 통과.
- 모듈 전체: `./gradlew :javers-core:test --no-build-cache --no-daemon
  --console=plain` — 209개 테스트 통과.
- 품질 게이트: `./gradlew :javers-core:check --no-build-cache --no-daemon
  --console=plain` — `check`, Kover verification 통과.
- `git diff --check` 통과.
- 이번 변경은 in-process SLF4J/Logback logger 계약만 다루며 DB, Redis, Kafka,
  네트워크 또는 외부 log collector 경계를 추가하지 않는다. 따라서
  `bluetape4k-testcontainers` 통합 테스트는 검증 대상이 없어 N/A다. 실제 broker,
  cache, database 또는 외부 collector 연동을 같은 변경에 포함할 때는 해당
  launcher를 사용한 통합 검증을 별도로 순차 실행해야 한다.

## 향후 지침

새 dispatcher나 운영 logger를 추가하거나 메시지 형식을 바꿀 때는 raw payload,
identifier, credential이 출력되지 않는지 실제 logger capture 회귀 테스트로
고정한다. 개발·테스트 전용 `ConsoleDispatcher`와 운영용 `Slf4jDispatcher`가
서로 다른 출력 경계를 갖더라도 동일한 비노출 정책을 유지한다. 외부 인프라나
collector acknowledgement를 포함하는 후속 작업은 단위 logger 테스트만으로
완료하지 말고 `bluetape4k-testcontainers` 기반 통합 테스트를 추가한다.
