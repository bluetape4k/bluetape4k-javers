# Issue #3 — javers-exposed 구현 계획

## Lane

Full Design / Type A: 새 Gradle module, public API, SQL persistence, Testcontainers, README 및 CI 변경.

## 작업

1. Module wiring
   - Exposed JDBC, H2, PostgreSQL, MySQL, Testcontainers DB module의 local catalog alias가 없으면 추가한다.
   - `settings.gradle.kts`에 `javers-exposed`를 포함한다.
   - `javers-exposed/build.gradle.kts`를 추가한다.
   - BOM module이 기존 root constraint를 통해 새 subproject를 포함하는지 확인한다.

2. Repository 구현
   - `CdoSnapshotTable`과 `CommitTable`을 추가한다.
   - `ExposedCdoSnapshotRepository`를 추가한다.
   - `ensureSchema`, `getKeys`, `contains`, `getSeq`, `updateCommitId`, `loadHeadId`, `getSnapshotSize`, `saveSnapshot`, `loadSnapshots`를 구현한다.
   - top-level Exposed operator(`eq`, `and`)를 사용하고 deprecated `SqlExpressionBuilder.eq`는 피한다.
   - public API에 English KDoc을 추가한다.

3. Tests
   - test resource `junit-platform.properties`, `logback-test.xml`을 추가한다.
   - JaVers core test fixture를 사용하는 H2 test base와 integration tests를 추가한다.
   - bluetape4k Testcontainers wrapper가 있으면 PostgreSQL 및 MySQL smoke tests에 사용하고, 없으면 raw `GenericContainer` 없이 official Testcontainers module을 사용한다.
   - rebuild/head restoration을 검증한다.

4. 문서 및 diagram
   - `javers-exposed/README.md`와 `README.ko.md`를 추가한다.
   - root README module table/build command를 갱신한다.
   - Exposed가 구현됐음을 표시하도록 `docs/assets/javers-persistence-options.svg/png`를 갱신한다.
   - lesson entry를 추가한다.

5. CI/Nightly
   - `.github/workflows/ci.yml`에 `javers-exposed` path filter와 job을 추가한다.
   - `.github/workflows/nightly-tests.yml`에 Nightly test와 coverage artifact를 추가한다.
   - `actionlint`를 실행한다.

6. 검증
   - `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-build-cache --no-parallel --console=plain`
   - `./gradlew :javers-exposed:cleanTest :javers-exposed:test --no-build-cache --no-parallel --console=plain`
   - `./gradlew build -x test --no-build-cache --no-parallel --console=plain`
   - `git diff --check`
   - `actionlint`
   - Render and visually inspect updated README PNG.

## 검토 Gate

- 구현 전에 spec/plan local/native 7-tier review를 수행한다.
- 검증 후 implemented diff review를 수행한다.
- Final DoD는 local/native review P0=0 및 P1=0을 보여야 한다.

## 중단 조건

module compile, targeted tests, README asset rendering, workflow YAML validation이 통과하고 review gate에 P0/P1 blocker가 없으면 중단한다.
