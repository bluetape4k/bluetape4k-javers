# 이슈 #178 - Kafka 키 진단 정보 비식별화

## 배경

PR #175 이후 스냅샷 페이로드 로그는 제거됐지만, Kafka 레코드 키는 추적 로그와
발행 실패 예외 메시지에 그대로 남아 있었다. JaVers 전역 ID는 이메일,
계좌 번호, 테넌트 ID 같은 자연 식별자를 포함할 수 있으므로 원본 키를
진단 정보에 남기면 안 된다.

## 결정

Kafka 레코드 키 자체는 전송 라우팅 계약이므로 그대로 유지한다. 대신 로그와
예외 메시지에는 원본 키, 접두사, 접미사, 마스킹된 키를 넣지 않고 다음 두 값만
남긴다.

- `keyFingerprint`: UTF-8 키의 SHA-256 16진수 접두사 16자.
- `keyLength`: 원본 키의 문자 길이.

Spring Kafka와 vanilla Kafka 어댑터는 같은 `KafkaSnapshotKeyDiagnostics` 포매터를
사용한다.

## 결과

- Spring Kafka / vanilla Kafka 저장소의 추적 로그에서 원본 키를 제거했다.
- Spring Kafka / vanilla Kafka 발행자의 실패/중단 메시지에서 원본 키를 제거했다.
- README 영어/한국어 문서에 키 진단 정책을 추가했다.
- 포매터 단위 테스트와 Spring/vanilla 로그/예외 회귀 테스트를 추가했다.

## 검증

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 통과, 테스트 32개.
- `git diff --check`
  - 통과.
- 소스 검사
  - Kafka 메인 소스에 원본 `key=$key` 진단 패턴이 없다.

## 향후 준수 사항

새 전송 어댑터가 스냅샷 이벤트 키를 로그, 예외, 메트릭 레이블, 추적 속성에
노출해야 한다면 원본 키를 쓰지 말고 `KafkaSnapshotKeyDiagnostics`와 같은 안정적인
지문/길이 정책을 먼저 정의해야 한다.
