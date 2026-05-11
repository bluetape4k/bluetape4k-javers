# Kover Coverage Policy

## Current Status

`bluetape4k-javers` aggregates Kover reports for core, Redis persistence, and
Kafka persistence modules. No module currently enforces `koverVerify`.

## Policy

Status: report-only transition.

Core diff/audit code and infrastructure persistence modules should have separate
thresholds because Redis/Kafka tests depend on external service behavior.

## Threshold Plan

- Gate `javers-core` first after baseline measurement.
- Add lower integration-heavy bounds for Redis and Kafka persistence modules
  after stable Nightly measurements.

## CI/Nightly Contract

Nightly uploads coverage artifacts. Add `koverVerify` only for modules with
validated thresholds.
