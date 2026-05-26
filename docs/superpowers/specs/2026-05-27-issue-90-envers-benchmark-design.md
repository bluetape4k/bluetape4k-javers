# Issue 90 Envers Comparison Benchmark Design

## Context

Parent #5 has two merged implementation slices:

- #88: command-side JaVers + Exposed DDD example.
- #89: Kafka to Redis projection and read-side API.

#90 adds measured benchmark documentation comparing JPA Envers with JaVers +
Exposed for insert, update, and audit-query scenarios.

Context7 was unavailable because the configured monthly quota was exceeded.
Official Hibernate references were checked from hibernate.org: Envers uses
`@Audited`, creates audit tables for audited entities, and exposes audit reads
through `AuditReader` / `AuditReaderFactory`.

## Goals

- Add a reproducible benchmark surface for the example module.
- Measure insert, update, and audit-query paths for:
  - Hibernate Envers on H2.
  - JaVers + Exposed on H2.
- Store raw benchmark output under `docs/benchmark/`.
- Update README.md and README.ko.md with command, environment, metric direction,
  and measured result table.

## Non-Goals

- Production performance claims.
- API or persistence optimization work.
- JMH adoption for this narrow documentation slice.

## Approach

Add a JUnit benchmark test under `examples/javers-exposed-ddd` that:

1. Performs a small warmup for both implementations.
2. Runs a fixed number of insert, update, and audit-query operations.
3. Uses `System.nanoTime()` around operation loops.
4. Writes deterministic JSON output to
   `docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json`.
5. Asserts that all scenario measurements are positive.

The benchmark is intentionally a documentation benchmark, not a microbenchmark.
It is fast enough for local reproduction and CI-compatible module testing.

## Acceptance Criteria

- Benchmark command completes within the requested 300 second timeout.
- Raw JSON benchmark output is committed.
- README.md and README.ko.md include measured data and caveats.
- `./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passes.
- `git diff --check` passes.

