# 의존성 카탈로그 업그레이드

## 배경

`bluetape4k-dependencies`에서 Apache Fory Dependabot PR을 중앙 의존성
업그레이드 작업 묶음에 통합했다.

## 결정

중앙에서 관리하는 Fory Kotlin 카탈로그 버전을 이 저장소에 반영한다.

## 결과

이제 `gradle/libs.versions.toml`에 Fory Kotlin `0.17.0`이 설정되어 있다.

## 검증

- `./gradlew build -x test --no-daemon`
