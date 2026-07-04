# Issue #232 javers-core coverage review

## Context
- Issue: #232 `test(javers-core): raise coverage above repo module average`
- Baseline module average: 88.09%
- Baseline `javers-core`: 3173/4357 instructions = 72.83%

## Changes
- Added focused tests for low-coverage utility surfaces:
  - entity envelopes and event type lookup
  - JaVers dispatchers and reified delete-by-id helper
  - commit metadata extensions
  - JQL query builders and query execution wrappers
  - Cdo snapshot property/filter helpers and wrapped Cdo access

## Result
- Updated `javers-core`: 3937/4357 instructions = 90.36%
- Delta: +17.53 percentage points, now above the 88.09% repository module average.

## Verification
- PASS: `./gradlew :javers-core:test :javers-core:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: Kover XML parsed from `javers-core/build/reports/kover/report.xml`
- N/A: `:javers-core:detekt` is not registered in this repository (`task 'detekt' not found in project ':javers-core'`).
