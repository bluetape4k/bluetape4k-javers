# JaVers + Exposed와 Hibernate Envers 비교

0.2.1의 벤치마크는 `javers-exposed-ddd` 테스트 모듈 안에서 두 감사 구현을 실행한 결과입니다. 숫자만 떼어 놓으면 잘못된 결론을 내리기 쉽습니다. 어떤 작업을 재었는지와 두 경로의 의미가 어떻게 다른지를 함께 봐야 합니다.

## 측정한 작업

[`EnversComparisonBenchmarkTest`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/EnversComparisonBenchmarkTest.kt)는 구현마다 새 인메모리 H2 데이터베이스를 만들고 세 시나리오를 실행합니다.

- **insert**: 준비용 삽입 5회 후 서로 다른 주문 40개를 저장합니다.
- **update**: 주문 40개를 미리 저장하고 각각 한 번씩 `PAID`로 바꿉니다.
- **audit-query**: 갱신한 ID 40개에 대해 감사 정보를 한 번씩 조회합니다.

측정은 순차 실행한 블록의 경과 시간을 `measureNanoTime`으로 잽니다. 각 시나리오는 전체 밀리초와 작업당 밀리초를 기록하며, 낮을수록 좋습니다.

산출물에 기록된 실행 명령은 다음과 같습니다.

```bash
./gradlew :javers-exposed-ddd:test --tests '*EnversComparisonBenchmarkTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain
```

## 측정 환경

0.2.1의 [JSON 산출물](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/docs/benchmark/2026-05-27-javers-exposed-ddd-envers-comparison.json)에는 다음 실행 정보가 기록돼 있습니다.

| 항목 | 값 |
|---|---|
| 생성 시각 | `2026-05-27T00:00:00Z` |
| Java | `21.0.11` |
| 운영체제 | macOS |
| 아키텍처 | `aarch64` |
| 준비 실행 설정 | 5, 삽입 측정 전에만 적용 |
| 측정 횟수 | 시나리오마다 40 |
| 측정 단위 | 작업당 밀리초, 낮을수록 좋음 |

JSON에는 데이터베이스 종류가 없습니다. 두 구현이 각각 새 인메모리 H2 데이터베이스를 사용한다는 사실은 릴리스의 벤치마크 테스트 소스에서 확인할 수 있습니다. JaVers 연결은 PostgreSQL 호환 모드를 켜고, Envers 연결은 해당 모드 없이 H2 방언을 사용합니다.

## 대표 측정값

| 시나리오 | Hibernate Envers 전체 ms | Envers ms/op | JaVers + Exposed 전체 ms | JaVers + Exposed ms/op |
|---|---:|---:|---:|---:|
| insert | 41.965 | 1.049 | 145.083 | 3.627 |
| update | 55.325 | 1.383 | 113.925 | 2.848 |
| audit-query | 320.414 | 8.010 | 4213.564 | 105.339 |

이 실행에서는 세 시나리오 모두 Envers가 빨랐고, 감사 조회에서 차이가 가장 컸습니다. 감사 조회가 응답 시간에 민감한 경로라면 실제 조회와 데이터 형태로 프로파일링해야 한다는 신호로 받아들이는 것이 적절합니다.

## 같은 기능을 재는 비교는 아니다

두 경로가 하는 일은 의미상 같지 않습니다.

- Envers 삽입과 갱신은 감사 대상 JPA 엔티티 하나와 리비전 행을 저장합니다.
- JaVers + Exposed 경로는 명령 측 `Order`를 저장하고 JaVers 커밋과 CDO 스냅샷을 만들며, 커밋에 도메인 이벤트 메타데이터도 넣습니다. 벤치마크에서는 무동작 발행기를 쓰므로 Kafka와 Redis 비용은 포함되지 않습니다.
- Envers 감사 시나리오는 리비전 ID 목록을 반환하는 `getRevisions`를 호출합니다.
- JaVers 시나리오는 `OrderRepository.loadHistory`로 애그리거트의 스냅샷 이력을 조회합니다.

따라서 이 결과는 하나의 정규화된 저장 작업을 서로 바꿔 끼울 수 있는 두 구현으로 비교한 것이 아닙니다. 0.2.1 예제에 들어 있는 두 경로를 그대로 실행한 결과입니다. Exposed, JaVers 직렬화, 커밋 메타데이터, 테이블 접근, Hibernate, Envers가 각각 얼마를 차지하는지도 이 측정만으로 분리할 수 없습니다.

## 이 결과로 말할 수 없는 것

이 산출물만으로 다음 내용을 주장할 수 없습니다.

- Envers가 언제나 빠르다거나 JaVers가 이 작업에 맞지 않는다는 결론
- 운영 PostgreSQL 또는 다른 데이터베이스에서의 성능과 동작
- 동시 명령·조회 환경의 처리량, 꼬리 지연 시간, 경합
- 여러 JVM 포크, 통제된 GC, 신뢰 구간을 포함한 안정 상태 성능
- 긴 감사 이력, 큰 객체 그래프, 사용자 정의 매핑, 인덱스, 캐시, 네트워크 지연의 영향
- Kafka 발행, Redis 프로젝션, 전체 명령 지연, 장애 복구 비용
- 두 구현의 감사 의미, 저장 용량, 조회 결과, 운영 복잡도가 같다는 가정
- 0.2.1 이후에 추가된 저장소 코드나 벤치마크 모듈의 성능

이 자료는 한 차례의 로컬 문서화 측정입니다. JMH 벤치마크나 릴리스 전체 성능 주장으로 해석하면 안 됩니다.

## 설계 판단에 활용하는 방법

서비스가 어떤 감사 질문에 답해야 하는지 먼저 정하세요. 엔티티 리비전 테이블과 리비전 ID 조회로 충분하다면 운영 매핑을 사용한 Envers 경로를 측정합니다. JaVers의 객체 변경 내역, 섀도, 커밋 속성, 애그리거트 중심 이력, DDD 명령 경로 연동이 필요하다면 그 기능을 유지한 채 비교해야 합니다. 더 작은 대체 작업을 재면 숫자는 좋아져도 설계 판단에는 도움이 되지 않습니다.

어느 쪽을 선택하든 다음 조건으로 다시 측정하는 편이 안전합니다.

1. 실제 PostgreSQL 버전과 운영에 가까운 스키마로 옮깁니다.
2. 현실적인 감사 이력 깊이와 객체 크기를 준비합니다.
3. 가능하면 두 조회 경로가 같은 업무 정보를 반환하도록 맞춥니다.
4. 여러 JVM 포크의 분포, 메모리 할당, DB 문장, 실행 계획을 함께 기록합니다.
5. 동시 쓰기 작업과 서비스에서 실제 노출할 감사 조회를 실행합니다.
6. 복구와 운영 비용은 요청 지연 시간과 분리해 평가합니다.

측정 원칙은 [벤치마크 근거 읽기](overview.md), 기능 흐름은 [0.2.1 예제](../examples/javers-exposed-ddd.md), 저장소 경계는 [Exposed 영속 방식](../persistence/exposed.md)에서 이어집니다.
