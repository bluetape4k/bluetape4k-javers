# Issue 153 Kover Coverage Reports

## Context

Issue #153 found two related workflow gaps:

- CI module test jobs uploaded test results but did not generate or upload Kover
  XML coverage reports.
- Nightly generated Kover reports with `continue-on-error: true` and uploaded
  broad Kover directories, so a broken report task or a directory without
  `report.xml` could pass as coverage evidence.

Issue #152 also showed that the Sunday full-scope schedule condition still
checked the old `0 19 * * 0` cron after the workflow moved to `15 19 * * 0`.

## Decision

Keep coverage percentage thresholds report-only, but make report generation
itself mandatory when a CI or Nightly module test job runs.

## Outcome

CI now runs each module's `koverXmlReport` task after the module test task and
uploads the module-specific `build/reports/kover/report.xml` artifact. Nightly no
longer hides Kover report generation failures, uploads only `report.xml`, and
validates that every expected full-scope artifact contains that XML report.

## Verification Evidence

- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `./gradlew :javers-core:koverXmlReport --no-configuration-cache --no-build-cache --no-daemon --console=plain`
- Workflow scan for Kover report steps, `if-no-files-found: error`, stale
  schedule conditions, and broad Kover upload paths
- Coverage artifact validation dry-run with all expected `report.xml` files and
  with one missing report
- `git diff --check`

## Future Guidance

When adding or renaming a module test job, update the CI Kover report step,
coverage artifact upload path, Nightly coverage artifact upload path, full-scope
coverage expected artifact list, and schedule predicates in one change.
