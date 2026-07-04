# Issue #118 Envers benchmark module review

## Scope

- Move the Envers comparison harness out of `examples/javers-exposed-ddd` test sources.
- Run it from `benchmark/javers-exposed-benchmark` with the existing `kotlinx-benchmark` surface.
- Keep example tests behavior-focused and keep README locale pairs aligned.
- Extend CI and Nightly benchmark smoke jobs to cover the new Envers benchmark task.

## 7-Tier Review

| Tier | Verdict | Evidence |
|---|---|---|
| 1. Correctness | PASS | `EnversComparisonBenchmark` now uses JMH state lifecycle and independent update targets per invocation, avoiding repeated paid-order transitions. |
| 2. API and module boundaries | PASS | The benchmark module owns the harness and declares explicit dependencies on the example module, `javers-ddd`, `javers-exposed`, and `hibernate-envers`; the example module no longer carries Envers test dependency. |
| 3. Data and transaction semantics | PASS | Benchmark trials create and drop temporary PostgreSQL-backed JaVers tables; production schema defaults are unchanged. |
| 4. Tests and silent failure | PASS | `mainEnversComparisonSmokeBenchmark` executed successfully with all benchmark variants and no JMH exception after the update-target fix. |
| 5. Build and CI registration | PASS | Gradle exposes `mainEnversComparisonSmokeBenchmark`; CI and Nightly benchmark jobs run both commit metadata and Envers smoke tasks. |
| 6. Documentation and locale parity | PASS | Root, example, and benchmark README locale pairs point to the benchmark module command and preserve raw result links. |
| 7. Release and maintainability | PASS | The ordinary example test suite no longer rewrites benchmark evidence; benchmark drift is now intentional through benchmark-module tasks. |

## Validation

- `./gradlew :benchmark-javers-exposed-benchmark:tasks --all --no-configuration-cache --console=plain`
  - Result: PASS, `mainEnversComparisonSmokeBenchmark` is present.
- `./gradlew :benchmark-javers-exposed-benchmark:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 5 tests.
- `./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, JMH summary emitted without `EXCEPTION`.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
  - Result: PASS, no output.
- `git diff --check`
  - Result: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
