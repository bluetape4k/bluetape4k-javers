# 이슈 114 bluetape4k 검증문 정리

## 배경

이슈 #114에서는 `javers-core` 테스트에 남아 있던
`org.junit.jupiter.api.assertThrows` import 문 한 건을 발견했다.

## 결정

동일한 `JaversException` 계약을 유지하면서 JUnit 예외 검증문을
`io.bluetape4k.assertions.assertFailsWith`로 교체한다.

## 결과

이제 `AbstractJaversShadowTest`의 예외 검증에도 bluetape4k 검증문을 일관되게
사용한다.

## 검증 증거

- `javers-core/src/test` 전체의 검증 API 검사
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`

## 향후 지침

테스트를 수정할 때는 작업 항목을 종료하기 전에 JUnit, AssertJ, Kluent 및
`kotlin.test` 검증문이 남아 있는지 검사한다.
