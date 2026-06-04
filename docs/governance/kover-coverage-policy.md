# Kover Coverage Policy

## Current Status

`bluetape4k-javers` generates Kover XML reports in CI for module test jobs and
aggregates full-scope Kover XML reports in Nightly. No module currently enforces
a failing coverage threshold.

## Policy

Status: report-only transition.

Core diff/audit code and infrastructure persistence modules should have separate
thresholds because Redis/Kafka tests depend on external service behavior.

## Threshold Plan

- Treat Kover as a trend signal, not a build gate.
- Use CI and Nightly XML reports plus coverage artifact uploads to identify
  coverage regressions.
- Open a focused issue when a module needs coverage repair; do not introduce a
  failing threshold as the default enforcement mechanism.

## CI/Nightly Contract

CI and Nightly upload coverage artifacts and keep trend visibility. They must
fail when a requested Kover XML report is missing or cannot be generated, but
must not fail solely because a module is below a fixed coverage percentage
unless a future issue explicitly reintroduces that gate.
