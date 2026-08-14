# 이슈 #313 ConsoleDispatcher payload 비노출

## 배경

`ConsoleDispatcher`가 `Any.toString()`과 삭제 ID를 `println` 문자열에 직접
삽입하고 있었습니다. 개발·테스트용 adapter라도 표준 출력이 로그 수집 경계로
연결되면 domain payload와 식별자가 운영 로그에 남을 수 있습니다.

## 결정

- `ConsoleDispatcher`는 개발·테스트 전용 adapter라는 운영 경계를 KDoc에 명시합니다.
- 저장·삭제 이벤트에는 객체의 안전한 타입 요약만 출력하고, ID 삭제 이벤트에는
  전달된 ID를 출력하지 않습니다.
- 타입 이름을 확인할 수 없는 anonymous type은 고정 문자열 `<anonymous>`로
  대체해 payload의 `toString()`이나 qualified name에 의존하지 않습니다.
- 기본 dispatcher 선택과 `JaversDispatcher` 공개 계약은 변경하지 않습니다.

## 결과

기존 세 이벤트의 console 출력은 유지하되, 출력 내용은 이벤트 종류와 타입
요약으로 제한됩니다. 민감 payload의 `toString()`과 민감 ID는 출력 결과에
포함되지 않습니다.

## 검증

- RED: 기존 구현에서 민감 객체와 ID를 전달한 회귀 테스트가 raw payload/ID
  노출로 2개 assertion을 실패시켰습니다.
- GREEN: `./gradlew :javers-core:test --tests
  'io.bluetape4k.javers.dispatcher.JaversDispatcherTest' --no-build-cache
  --no-daemon --console=plain` 통과.
- 전체 모듈: `./gradlew :javers-core:test --no-build-cache --no-daemon
  --console=plain` 실행 결과 208개 테스트, failures/errors/skipped 0.
- `git diff --check` 통과.
- 이번 변경은 `println`과 in-memory `ByteArrayOutputStream`만 다루며 DB, Redis,
  Kafka, 네트워크 또는 persistence lifecycle을 포함하지 않습니다. 따라서
  `bluetape4k-testcontainers` 통합 fixture를 추가해도 이 payload 비노출 계약의
  증거가 늘지 않아 N/A로 판정했습니다.

## 향후 지침

console/log adapter를 추가하거나 출력 형식을 변경할 때는 객체의 `toString()`과
raw identifier가 출력되지 않는 회귀 테스트를 함께 작성합니다. 외부 broker,
DB, cache 또는 네트워크 경계를 실제로 포함하는 변경이라면 해당 경계는
`bluetape4k-testcontainers` 기반 통합 테스트로 별도 검증합니다.
