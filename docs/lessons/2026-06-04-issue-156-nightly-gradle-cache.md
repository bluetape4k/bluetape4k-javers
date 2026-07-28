# 2026-06-04 이슈 156 Nightly Gradle 캐시

## 배경

bluetape4k 저장소의 Nightly 빌드가 GitHub 실행기에서 관리 대상 의존성을
간헐적으로 `group:artifact:.`로 해석했다.

## 결정

예약 실행에서 오래된 의존성 관리 상태를 재사용하지 않도록 Nightly 작업의
`gradle/actions/setup-gradle` 캐시 복원과 쓰기를 비활성화한다.

## 결과

이제 모든 Nightly `setup-gradle` 블록에서 명시적인 Gradle 의존성 새로 고침을
유지하면서 `cache-disabled: true`를 설정한다.

## 검증

- `.github/workflows/nightly-tests.yml` 감사 결과: `setup-gradle` 블록 수와
  `cache-disabled` 블록 수가 일치한다.
- 계획된 검증: `actionlint`, `git diff --check`.

## 향후 규칙

Nightly 워크플로가 snapshot 또는 BOM으로 관리되는 bluetape4k 의존성을 사용할
때는 캐시 복원으로 오래된 메타데이터가 재사용되지 않는다는 최신 CI 증거가
확보되지 않는 한 Gradle 작업 캐시를 비활성화한다.
