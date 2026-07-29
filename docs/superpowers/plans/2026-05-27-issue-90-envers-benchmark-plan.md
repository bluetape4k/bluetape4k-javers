# Issue 90 Envers Comparison Benchmark Plan

## 단계

1. `examples/javers-exposed-ddd`에 test-scope benchmark dependency를 추가한다.
   - central `bt4k` catalog의 Hibernate Envers.
   - central `bt4k` catalog의 Jakarta Persistence API.

2. `EnversComparisonBenchmarkTest`를 추가한다.
   - H2를 사용하는 programmatic Hibernate Envers setup.
   - H2를 사용하는 기존 JaVers + Exposed setup.
   - insert, update, audit-query용 warmup 및 measured loop.
   - `docs/benchmark/` 아래 JSON artifact writer.

3. benchmark command를 실행하고 raw output을 commit한다.

4. docs를 갱신한다.
   - README.md / README.ko.md benchmark section.
   - WIP.md marks #90 active/completed state.
   - Lesson entry for future agents.

5. 검증한다.
   - targeted benchmark test.
   - full example module test.
   - `git diff --check`.
   - P0/P1=0인 Local 7-Tier review.

## 위험

- timing은 local machine과 H2 behavior에 의존한다. README는 이를 universal performance claim이 아니라 local documentation benchmark result로 설명해야 한다.
- Envers와 JaVers는 서로 다른 일을 한다. 비교는 scenario를 단순하게 유지하고 metric direction이 lower milliseconds per operation임을 명시해야 한다.
- iteration count가 너무 크면 benchmark tests는 flaky해질 수 있다. count를 bounded하게 유지하고 per-operation assertion 대신 total-loop timing을 사용한다.
