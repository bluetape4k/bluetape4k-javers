# 이슈 #160 Central 스냅샷 재시도

## 배경
GitHub 러너가 Central Portal 스냅샷 메타데이터에서 일시적인 HTTP 403 응답을 받으면
하위 CI 및 Nightly 실행이 실패할 수 있다.

## 결정
Gradle 명령의 의미는 변경하지 않고 최상위 Gradle 빌드와 Nightly detekt 게이트를
최대 세 번 시도하는 제한된 재시도 루프로 감싼다.

## 검증
- `git diff --check`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`

## 다음 대응
bluetape4k SNAPSHOT 의존성이 Central 메타데이터 403으로 실패하면 먼저 상위 프로젝트의
게시 상태를 확인한다. 그런 다음 의존성이나 카탈로그를 불필요하게 변경하기보다 제한된
워크플로 재시도를 우선한다.
