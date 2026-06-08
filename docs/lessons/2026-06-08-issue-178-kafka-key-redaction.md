# Issue #178 - Kafka key diagnostics redaction

## Context

PR #175 이후 snapshot payload 로그는 제거됐지만, Kafka record key는 trace log와
publish failure exception message에 그대로 남아 있었다. JaVers global id는 email,
account number, tenant id 같은 natural identifier를 포함할 수 있으므로 raw key는
diagnostic surface에 남기면 안 된다.

## Decision

Kafka record key 자체는 transport routing contract라 그대로 유지한다. 대신 log와
exception message에는 raw key, prefix, suffix, masked key를 넣지 않고 다음 두 값만
남긴다.

- `keyFingerprint`: UTF-8 key의 SHA-256 hex prefix 16자.
- `keyLength`: raw key의 character length.

Spring Kafka와 vanilla Kafka adapter는 같은 `KafkaSnapshotKeyDiagnostics` formatter를
사용한다.

## Outcome

- Spring Kafka / vanilla Kafka repository trace log에서 raw key를 제거했다.
- Spring Kafka / vanilla Kafka publisher failure/interruption message에서 raw key를 제거했다.
- README 영어/한국어 문서에 key diagnostics 정책을 추가했다.
- formatter 단위 테스트와 Spring/vanilla 로그/예외 regression test를 추가했다.

## Verification

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 32 tests.
- `git diff --check`
  - PASS.
- Source scan
  - raw `key=$key` diagnostic pattern is absent from Kafka main sources.

## Future Guard

새 transport adapter가 snapshot event key를 log, exception, metric label, trace attribute에
노출해야 한다면 raw key를 쓰지 말고 `KafkaSnapshotKeyDiagnostics`와 같은 stable
fingerprint/length 정책을 먼저 정의해야 한다.
