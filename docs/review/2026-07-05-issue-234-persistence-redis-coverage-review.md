# Issue #234 persistence-redis coverage review

## Context
- Issue: #234 `test(persistence-redis): raise coverage above repo module average`
- Baseline module average: 88.09%
- Baseline `persistence-redis`: 753/1078 instructions = 69.85%

## Changes
- Added focused Lettuce Redis repository tests for previously undercovered backend contracts:
  - direct `saveSnapshot` appends encoded snapshots and updates the key index
  - `projectSnapshot` restores projected snapshot and commit sequence metadata
  - sequence metadata can be restored independently during head rebuild
  - failed Lettuce `EXEC` discards the transaction and propagates the write failure

## Result
- Updated `persistence-redis`: 950/1078 instructions = 88.13%
- Delta: +18.28 percentage points, now above the 88.09% repository module average.

## Verification
- PASS: `./gradlew :javers-persistence-redis:test :javers-persistence-redis:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: `./gradlew :javers-persistence-redis:check :javers-persistence-redis:koverXmlReport --no-daemon --no-configuration-cache`
- PASS: Kover XML parsed from `javers-persistence-redis/build/reports/kover/report.xml`
