# 2026-06-04 이슈 158 Nightly 구성 캐시와 카탈로그

## 배경

Nightly 및 CI의 컴파일 전용 워크플로는 스냅샷과 BOM으로 관리하는 의존성을 사용한다. 따라서 오래된 Gradle 또는 구성 상태로 인해 버전이 없는 의존성 좌표가 나타나거나 일시적인 스냅샷 메타데이터 장애가 확대될 수 있다.

## 결정

Nightly 및 CI에서 새로 고침과 컴파일을 수행하는 Gradle 명령에는 `--no-configuration-cache`를 유지한다. 또한 로컬 bluetape4k 별칭에는 저장소에서 정의한 버전 키를 통해 버전을 계속 지정한다.

## 결과

Nightly 및 CI의 컴파일 전용 명령은 의존성을 새로 고칠 때 더 이상 구성 캐시에 의존하지 않으며, 저장소 로컬 카탈로그 별칭은 `group:artifact:.` 좌표를 생성하지 않는다.

## 검증

- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`: 통과.
- `git diff --check`: 통과.
- 버전 참조 감사: 누락된 `version.ref` 키가 없음.
- `./gradlew build -x test --parallel --no-configuration-cache --no-daemon`: 통과.

## 향후 규칙

스냅샷 의존성을 새로 고치거나 컴파일하는 Nightly 또는 CI 작업에서는 저장소별 근거가 달리 입증하지 않는 한 Gradle 액션 캐시와 구성 캐시를 모두 비활성화한다.
