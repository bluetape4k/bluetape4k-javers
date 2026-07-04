# Issue #118 Envers benchmark module

## Context

The Envers comparison lived in `examples/javers-exposed-ddd` test sources, so
ordinary example tests could rewrite benchmark evidence. The repository already
had `benchmark/javers-exposed-benchmark` for `kotlinx-benchmark` smoke runs.

## Decision

Move the Envers comparison to the benchmark module and expose it through
`mainEnversComparisonSmokeBenchmark`. Keep the historical committed JSON as a
snapshot, but document fresh execution through the benchmark module.

## Outcome

The example module no longer depends on `hibernate-envers` for tests. CI and
Nightly benchmark jobs now run both commit metadata and Envers smoke tasks when
the benchmark module changes.

## Future Guidance

Do not add new benchmark harnesses to example test sources. Put them under a
benchmark module and verify the generated `kotlinx-benchmark` task name before
updating README or workflow commands.
