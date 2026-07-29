# Issue #232 javers-core coverage review

## Context 요약
- Issue: #232 `test(javers-core): raise coverage above repo module average`
- Baseline module average: 88.09%
- Baseline `javers-core`: 3173/4357 instructions = 72.83%

## 변경
- low-coverage utility surface에 focused tests를 추가했다.
  - entity envelopes and event type lookup
  - JaVers dispatchers and reified delete-by-id helper
  - commit metadata extensions
  - JQL query builders and query execution wrappers
  - Cdo snapshot property/filter helpers and wrapped Cdo access

## 결과
- Updated `javers-core`: 3937/4357 instructions = 90.36%
- Delta: +17.53 percentage points, 이제 88.09% repository module average를 넘는다.

## 검증
- PASS: `./gradlew :javers-core:test :javers-core:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: Kover XML parsed from `javers-core/build/reports/kover/report.xml`
- N/A: `:javers-core:detekt` is not registered in this repository (`task 'detekt' not found in project ':javers-core'`).
