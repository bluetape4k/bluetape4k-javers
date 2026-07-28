# Issue 153 Kover 커버리지 보고서

## 배경

Issue #153에서 서로 연관된 두 가지 워크플로 공백을 확인했다.

- CI 모듈 테스트 작업은 테스트 결과를 업로드했지만 Kover XML 커버리지 보고서를
  생성하거나 업로드하지 않았다.
- Nightly는 `continue-on-error: true`로 Kover 보고서를 생성하고 Kover 디렉터리
  전체를 업로드했다. 따라서 보고서 작업이 실패하거나 `report.xml`이 없는
  디렉터리도 커버리지 근거로 통과할 수 있었다.

Issue #152에서는 워크플로가 `15 19 * * 0`으로 변경된 뒤에도 일요일 전체 범위
일정 조건이 이전 cron 표현식인 `0 19 * * 0`을 계속 확인하는 문제도 드러났다.

## 결정

커버리지 비율 임곗값은 보고 목적으로만 유지하되, CI 또는 Nightly 모듈 테스트
작업이 실행될 때 보고서 생성 자체는 필수로 한다.

## 결과

이제 CI는 각 모듈의 테스트 작업 후 `koverXmlReport` 작업을 실행하고 모듈별
`build/reports/kover/report.xml` 아티팩트를 업로드한다. Nightly는 더 이상 Kover
보고서 생성 실패를 숨기지 않으며, `report.xml`만 업로드하고 전체 범위에서
예상되는 모든 아티팩트에 해당 XML 보고서가 포함되어 있는지 검증한다.

## 검증 근거

- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `./gradlew :javers-core:koverXmlReport --no-configuration-cache --no-build-cache --no-daemon --console=plain`
- Kover 보고서 단계, `if-no-files-found: error`, 오래된 일정 조건, 광범위한 Kover
  업로드 경로를 대상으로 한 워크플로 검사
- 예상되는 `report.xml` 파일이 모두 있는 경우와 보고서 하나가 누락된 경우를
  대상으로 한 커버리지 아티팩트 검증 dry-run
- `git diff --check`

## 향후 지침

모듈 테스트 작업을 추가하거나 이름을 변경할 때는 CI Kover 보고서 단계,
커버리지 아티팩트 업로드 경로, Nightly 커버리지 아티팩트 업로드 경로, 전체
범위에서 예상되는 커버리지 아티팩트 목록, 일정 조건식을 하나의 변경으로 함께
갱신한다.
