# Kover Coverage Policy

## Current Status

`bluetape4k-javers` aggregates Kover reports for core, Redis persistence, and
Kafka persistence modules. No module currently enforces a failing coverage
threshold.

## Policy

Status: report-only transition.

Core diff/audit code and infrastructure persistence modules should have separate
thresholds because Redis/Kafka tests depend on external service behavior.

## Threshold Plan

- Treat Kover as a trend signal, not a build gate.
- Use Nightly XML reports and existing coverage artifact uploads to identify
  coverage regressions.
- Open a focused issue when a module needs coverage repair; do not introduce a
  failing threshold as the default enforcement mechanism.

## CI/Nightly Contract

Nightly uploads coverage artifacts and keeps trend visibility. CI and Nightly
must not fail solely because a module is below a fixed coverage percentage
unless a future issue explicitly reintroduces that gate.
