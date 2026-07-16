# Benchmark 근거 읽기

0.2 매뉴얼은 벤치마크 결과를 제품 순위표로 쓰지 않습니다. 어떤 작업을 어느 환경에서 실행했는지, 수치가 어느 방향으로 좋다고 해석하는지, 다른 환경에서 결과가 달라질 조건은 무엇인지까지 함께 기록해야 쓸모 있는 근거가 됩니다.

## 0.2.1에 포함된 측정 자료

0.2.1 릴리스에는 벤치마크 산출물이 하나 있습니다.

- [`2026-05-27-javers-exposed-ddd-envers-comparison.json`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json)

이 파일은 같은 릴리스의 [`EnversComparisonBenchmarkTest`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt)가 만들었습니다. 0.2.1 이후에 추가된 전용 벤치마크 모듈이나 결과 파일은 이 매뉴얼의 근거로 사용하지 않습니다.

## 결과를 읽는 순서

먼저 측정 작업이 답하는 질문부터 확인합니다. 이 자료는 Hibernate Envers와 JaVers + Exposed의 작은 H2 영속 작업을 비교합니다. 삽입, 갱신, 감사 조회 한 종류를 작업당 밀리초로 측정했으며 낮을수록 좋습니다. Kafka에서 Redis로 이어지는 프로젝션, 운영 PostgreSQL, 전체 명령 지연, 동시 처리량, 메모리 할당, 시작 시간, 장애 복구는 측정하지 않았습니다.

다음으로 환경과 반복 횟수를 봅니다. 측정 환경은 Java `21.0.11`, macOS `aarch64`입니다. 시나리오마다 준비 실행 5회 후 40회를 측정했습니다. 로컬 문서화를 위한 측정이므로 통계적으로 충분한 성능 연구라고 볼 수 없습니다.

마지막으로 숫자에서 확인할 가설을 고릅니다. 이 실행에서는 좁게 정의한 세 시나리오 모두 Envers가 빨랐습니다. 같은 작업에서 JaVers + Exposed 비용을 더 살펴봐야 한다는 근거는 됩니다. 하지만 어느 감사 모델이 모든 상황에서 낫다는 뜻은 아닙니다. 두 구현이 제공하는 모델과 연동 지점이 서로 다르기 때문입니다.

## 결정 전에 다시 측정하기

산출물에 기록된 명령은 다음과 같습니다.

```bash
./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

성능을 선택 기준으로 삼으려면 실제 JDK, OS, 데이터베이스, 데이터 규모, 조회 형태로 다시 실행하세요. 결정 영향이 크다면 원본 산출물을 남기고, 여러 JVM 포크의 분포를 비교하며, 준비 시간과 안정 상태 작업 시간을 분리해 측정해야 합니다.

세부 수치와 제약은 [JaVers + Exposed와 Envers 비교](exposed-ddd-envers.md)에, 기능 흐름은 [0.2.1 주문 예제](../examples/javers-exposed-ddd.md)에 정리돼 있습니다.
