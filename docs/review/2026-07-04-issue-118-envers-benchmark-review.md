# Issue #118 Envers benchmark module review

## 범위

- Envers comparison harness를 `examples/javers-exposed-ddd` test source 밖으로 이동한다.
- 기존 `kotlinx-benchmark` surface로 `benchmark/javers-exposed-benchmark`에서 실행한다.
- example tests는 behavior-focused로 유지하고 README locale pair를 맞춘다.
- CI 및 Nightly benchmark smoke job을 확장해 새 Envers benchmark task를 커버한다.

## 7-Tier 검토

| Tier | 판정 | 증거 |
|---|---|---|
| 1. Correctness | PASS | `EnversComparisonBenchmark`는 이제 JMH state lifecycle과 invocation별 independent update target을 사용해 repeated paid-order transition을 피한다. |
| 2. API and module boundaries | PASS | benchmark module이 harness를 소유하고 example module, `javers-ddd`, `javers-exposed`, `hibernate-envers`에 대한 explicit dependency를 declare한다. example module은 더 이상 Envers test dependency를 갖지 않는다. |
| 3. Data and transaction semantics | PASS | Benchmark trial은 temporary PostgreSQL-backed JaVers table을 만들고 drop한다. production schema default는 변경하지 않았다. |
| 4. Tests and silent failure | PASS | update-target fix 이후 `mainEnversComparisonSmokeBenchmark`가 모든 benchmark variant에서 JMH exception 없이 성공했다. |
| 5. Build and CI registration | PASS | Gradle은 `mainEnversComparisonSmokeBenchmark`를 노출한다. CI 및 Nightly benchmark job은 commit metadata와 Envers smoke task를 모두 실행한다. |
| 6. Documentation and locale parity | PASS | Root, example, benchmark README locale pair는 benchmark module command를 가리키고 raw result link를 보존한다. |
| 7. Release and maintainability | PASS | 일반 example test suite는 더 이상 benchmark evidence를 다시 쓰지 않는다. benchmark drift는 이제 benchmark-module task를 통한 의도적 동작이다. |

## 검증

- `./gradlew :benchmark-javers-exposed-benchmark:tasks --all --no-configuration-cache --console=plain`
  - 결과: PASS, `mainEnversComparisonSmokeBenchmark` is present.
- `./gradlew :benchmark-javers-exposed-benchmark:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
- `./gradlew :examples-javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 5 tests.
- `./gradlew :benchmark-javers-exposed-benchmark:mainEnversComparisonSmokeBenchmark --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, JMH summary emitted without `EXCEPTION`.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
  - 결과: PASS, no output.
- `git diff --check`
  - 결과: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
