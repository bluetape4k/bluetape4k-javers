# 이슈 90 Envers 벤치마크 교훈

## 배경

#90은 #88과 #89가 반영된 후 `javers-exposed-ddd` 예제의 벤치마크 및 문서화
작업을 마무리한다.

## 결정

완전한 JMH 모듈을 추가하는 대신 범위가 제한된 JUnit 문서화 벤치마크를 사용한다.
벤치마크는 원시 JSON을 `docs/benchmark/` 아래에 기록하고, README 표에서 생성된
아티팩트를 추적할 수 있게 한다.

## 결과

로컬 H2 벤치마크의 제한된 삽입, 갱신, 감사 쿼리 시나리오에서는 Hibernate
Envers가 JaVers + Exposed보다 빨랐다. README에는 이 결과를 명확히 밝히고, JaVers
예제의 초점을 단순 H2 처리량 대신 명시적인 애그리거트 커밋, 메타데이터, 도메인
이벤트, CQRS 프로젝션 통합에 둔다.

## 검증

- `./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  실행이 통과했고 원시 벤치마크 아티팩트를 생성했다.

## 향후 지침

벤치마크 근거를 홍보 문구로 바꾸지 않는다. 나중에 성능에 관한 주장이 필요하면
전용 JMH 벤치마크 모듈을 추가하고 더 큰 데이터셋에서 동등한 감사 읽기 형태를
비교한다.
