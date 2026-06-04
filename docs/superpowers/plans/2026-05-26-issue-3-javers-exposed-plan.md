# Issue #3 — javers-exposed Implementation Plan

## Lane

Full Design / Type A: new Gradle module, public API, SQL persistence, Testcontainers, README and CI changes.

## Tasks

1. Module wiring
   - Add local catalog aliases for Exposed JDBC, H2, PostgreSQL, MySQL, and Testcontainers DB modules if absent.
   - Include `javers-exposed` in `settings.gradle.kts`.
   - Add `javers-exposed/build.gradle.kts`.
   - Ensure BOM module picks up the new subproject through existing root constraints.

2. Repository implementation
   - Add `CdoSnapshotTable` and `CommitTable`.
   - Add `ExposedCdoSnapshotRepository`.
   - Implement `ensureSchema`, `getKeys`, `contains`, `getSeq`, `updateCommitId`, `loadHeadId`, `getSnapshotSize`, `saveSnapshot`, and `loadSnapshots`.
   - Use top-level Exposed operators (`eq`, `and`) and avoid deprecated `SqlExpressionBuilder.eq`.
   - Add English KDoc for public API.

3. Tests
   - Add test resources: `junit-platform.properties`, `logback-test.xml`.
   - Add H2 test base and integration tests using JaVers core test fixtures.
   - Add PostgreSQL and MySQL smoke tests with bluetape4k Testcontainers wrappers if available; otherwise use official Testcontainers modules without raw `GenericContainer`.
   - Verify rebuild/head restoration.

4. Documentation and diagrams
   - Add `javers-exposed/README.md` and `README.ko.md`.
   - Update root README module table/build commands.
   - Update `docs/assets/javers-persistence-options.svg/png` to mark Exposed as implemented.
   - Add a lesson entry.

5. CI/Nightly
   - Add `javers-exposed` path filter and job in `.github/workflows/ci.yml`.
   - Add Nightly test and coverage artifact in `.github/workflows/nightly-tests.yml`.
   - Run `actionlint`.

6. Verification
   - `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-build-cache --no-parallel --console=plain`
   - `./gradlew :javers-exposed:cleanTest :javers-exposed:test --no-build-cache --no-parallel --console=plain`
   - `./gradlew build -x test --no-build-cache --no-parallel --console=plain`
   - `git diff --check`
   - `actionlint`
   - Render and visually inspect updated README PNG.

## Review Gates

- Spec/plan local/native 7-tier review before implementation.
- Implemented diff review after verification.
- Final DoD must show local/native review P0=0 and P1=0.

## Stop Condition

Stop when module compiles, targeted tests pass, README assets render, workflow YAML validates, and review gates have no P0/P1 blockers.
