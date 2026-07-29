# Issue 90 Envers Comparison Benchmark 설계

## 맥락

Parent #5에는 merge된 implementation slice 두 개가 있다.

- #88: command-side JaVers + Exposed DDD example.
- #89: Kafka to Redis projection and read-side API.

#90은 insert, update, audit-query scenario에서 JPA Envers와 JaVers + Exposed를 비교하는 measured benchmark documentation을 추가한다.

Context7은 configured monthly quota 초과로 사용할 수 없었다. Official Hibernate reference는 hibernate.org에서 확인했다. Envers는 `@Audited`를 사용하고 audited entity용 audit table을 만들며 `AuditReader` / `AuditReaderFactory`를 통해 audit read를 노출한다.

## 목표

- Example module용 reproducible benchmark surface를 추가한다.
- 다음의 insert, update, audit-query path를 측정한다.
  - Hibernate Envers on H2.
  - JaVers + Exposed on H2.
- Raw benchmark output을 `docs/benchmark/` 아래 저장한다.
- README.md와 README.ko.md에 command, environment, metric direction, measured result table을 추가한다.

## Non-goal

- Production performance claim.
- API 또는 persistence optimization work.
- 이 좁은 documentation slice를 위한 JMH adoption.

## 접근

`examples/javers-exposed-ddd` 아래 다음을 수행하는 JUnit benchmark test를 추가한다.

1. 두 implementation에 작은 warmup을 수행한다.
2. 고정된 수의 insert, update, audit-query operation을 실행한다.
3. Operation loop 주변에서 `System.nanoTime()`을 사용한다.
4. Deterministic JSON output을 다음 경로에 쓴다.
   `docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json`.
5. 모든 scenario measurement가 positive인지 assert한다.

이 benchmark는 microbenchmark가 아니라 의도적으로 documentation benchmark다. Local reproduction과 CI-compatible module testing에 충분히 빠르다.

## 인수 기준

- Benchmark command가 요청된 300초 timeout 안에 완료된다.
- Raw JSON benchmark output이 commit된다.
- README.md와 README.ko.md가 measured data 및 caveat를 포함한다.
- `./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  가 통과한다.
- `git diff --check`가 통과한다.
