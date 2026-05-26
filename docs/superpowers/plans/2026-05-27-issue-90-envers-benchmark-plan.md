# Issue 90 Envers Comparison Benchmark Plan

## Steps

1. Add test-scope benchmark dependencies to `examples/javers-exposed-ddd`.
   - Hibernate Envers from the central `bt4k` catalog.
   - Jakarta Persistence API from the central `bt4k` catalog.

2. Add `EnversComparisonBenchmarkTest`.
   - Programmatic Hibernate Envers setup with H2.
   - Existing JaVers + Exposed setup with H2.
   - Warmup and measured loops for insert, update, and audit-query.
   - JSON artifact writer under `docs/benchmark/`.

3. Run benchmark command and commit raw output.

4. Update docs.
   - README.md / README.ko.md benchmark section.
   - WIP.md marks #90 active/completed state.
   - Lesson entry for future agents.

5. Verify.
   - Targeted benchmark test.
   - Full example module test.
   - `git diff --check`.
   - Local 7-Tier review with P0/P1=0.

## Risks

- Timings depend on local machine and H2 behavior. README must describe them as
  local documentation benchmark results, not universal performance claims.
- Envers and JaVers do different work. The comparison must keep the scenario
  simple and state that metric direction is lower milliseconds per operation.
- Benchmark tests can become flaky if iteration counts are too large. Keep
  counts bounded and use total-loop timing rather than per-operation assertions.

