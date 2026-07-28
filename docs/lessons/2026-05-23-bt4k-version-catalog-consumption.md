# bt4k 버전 카탈로그 사용

## 배경

`bluetape4k-javers`는 이미 생태계 공용 카탈로그에서 관리하는 공통 의존성
버전을 로컬에서 별도로 고정하고 있었다.

## 결정

`bluetape4k-dependencies` 버전 카탈로그를 `bt4k`라는 이름으로 가져오고,
의존성 관리에서 공용 하위 의존성 버전을 `bt4kVersion(alias)`로 조회한다.

## 결과

선택한 의존성 별칭은 로컬 카탈로그에 버전을 지정하지 않으며, Gradle 의존성
관리 과정에서 공용 카탈로그의 버전을 사용한다.

## 검증

- `git diff --check`
- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew compileKotlin --no-daemon --no-configuration-cache`

## 향후 지침

직렬화, 캐시, 로깅 및 압축 관련 공통 의존성 버전을 로컬에서 고정하기 전에
`bt4k`를 사용한다.
