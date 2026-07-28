# Issue #234 persistence-redis coverage review

## Context 요약
- Issue: #234 `test(persistence-redis): raise coverage above repo module average`
- Baseline module average: 88.09%
- Baseline `persistence-redis`: 753/1078 instructions = 69.85%

## 변경
- 이전에 coverage가 부족했던 backend contract를 위해 focused Lettuce Redis repository tests를 추가했다.
  - direct `saveSnapshot` appends encoded snapshots and updates the key index
  - `projectSnapshot` restores projected snapshot and commit sequence metadata
  - sequence metadata can be restored independently during head rebuild
  - failed Lettuce `EXEC` discards the transaction and propagates the write failure

## 결과
- Updated `persistence-redis`: 950/1078 instructions = 88.13%
- Delta: +18.28 percentage points, 이제 88.09% repository module average를 넘는다.

## 검증
- PASS: `./gradlew :javers-persistence-redis:test :javers-persistence-redis:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :javers-persistence-redis:check :javers-persistence-redis:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: Kover XML parsed from `javers-persistence-redis/build/reports/kover/report.xml`
