# 압축 codec의 malformed decode 계약과 검증 경계

**관련 이슈**: [#307](https://github.com/bluetape4k/bluetape4k-javers/issues/307)
**영향 모듈**: `javers-core`
**작성일**: 2026-08-12

## 배경

`JaversCodec.decode()`는 encode된 값이 손상되었거나 형식이 맞지 않으면 `null`을
반환하는 공통 계약을 가진다. `StringJaversCodec`는 parse failure를 `null`로
정규화하지만, 압축 wrapper는 `compressor.decompress()`의 예외를 그대로 전파했다.
따라서 malformed compressed snapshot 하나가 repository의 전체 read/replay 흐름을
중단시킬 수 있었다.

## 근본 원인

`CompressibleStringJaversCodec`와 `CompressibleBinaryJaversCodec`가 압축 해제와 내부
codec decode를 하나의 예외 경계 없이 직접 연결했다. 압축 라이브러리의
`ZipException`, `IllegalArgumentException`, algorithm-specific runtime failure가
상위 `JaversCodec`의 nullable failure contract로 정규화되지 않았다.

## 결정

- 두 compressed codec 모두 압축 해제/format `Exception`을 `null`로 정규화한다.
- `CancellationException`은 삼키지 않고 다시 전파한다.
- raw payload를 로그에 남기지 않는다. 이 deterministic codec 경계에는 별도
  운영 로그가 필요하지 않으며, caller는 `null` 결과로 손상 snapshot을 건너뛴다.
- README와 KDoc에 compressed payload도 동일한 `null` decode 계약을 기록한다.

## 검증 경계

이 변경은 Redis, Kafka, database, HTTP 같은 외부 인프라와 통신하지 않는 순수
in-memory codec 변환이다. 따라서 `bluetape4k-testcontainers` launcher를 붙이는
것은 실제 경계를 추가로 검증하지 못하고 scope와 dependency만 넓힌다. 대신 실제
compressed codec을 Caffeine snapshot repository caller에 연결해 malformed entry가
예외 없이 `mapNotNull` 경로에서 건너뛰는 통합 수준 테스트를 추가했다.

향후 codec이 Redis/Kafka/DB adapter의 serialization 경계를 직접 변경하면 이
N/A 판단을 재사용하지 말고 해당 `bluetape4k-testcontainers` launcher를 순차
실행해야 한다.

## 검증

- RED: malformed GZip String/Binary와 repository caller 테스트 3개가
  `ZipException: Not in GZIP format`으로 실패했다.
- GREEN: 동일 테스트 3개가 통과했다.
- `:javers-core:check --rerun-tasks --no-build-cache --no-daemon --console=plain`:
  207개 테스트, Kover 검증, `BUILD SUCCESSFUL`.
- 기존 `JaversCodecTest`의 String/Binary codec parameterized round-trip matrix가
  full module check에서 함께 통과했다.
- `git diff --check`: 통과.

## 재사용 규칙

codec contract를 변경할 때는 먼저 malformed payload를 실제 caller 경계에서
재현하고, 그 테스트가 RED가 된 뒤 최소 production 변경으로 GREEN을 만든다.
순수 in-memory 경계에는 Testcontainers를 형식적으로 추가하지 않되, 외부 adapter
serialization 경계가 바뀌면 해당 launcher를 이용한 통합 테스트를 생략하지 않는다.
