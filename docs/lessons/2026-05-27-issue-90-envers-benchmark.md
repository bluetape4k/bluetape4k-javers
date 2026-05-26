# Issue 90 Envers Benchmark Lesson

## Context

#90 closes the benchmark/documentation slice for the `javers-exposed-ddd`
example after #88 and #89 landed.

## Decision

Use a bounded JUnit documentation benchmark instead of adding a full JMH module.
The benchmark writes raw JSON under `docs/benchmark/` and keeps the README table
traceable to the generated artifact.

## Outcome

The local H2 benchmark showed Hibernate Envers faster than JaVers + Exposed for
the narrow insert, update, and audit-query scenarios. README copy states that
plainly and frames the JaVers example around explicit aggregate commits,
metadata, domain events, and CQRS projection integration instead of raw H2
throughput.

## Verification

- `./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  passed and generated the raw benchmark artifact.

## Future Guidance

Do not turn benchmark evidence into a marketing claim. If performance claims are
needed later, add a dedicated JMH benchmark module and compare equivalent audit
read shapes with larger datasets.
