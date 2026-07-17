# 저장소 지도

`bluetape4k-javers`는 JaVers 감사와 이력의 의미를 맡습니다. 현재 업무 상태를 저장하는 애플리케이션 저장소까지 대신하지는 않습니다. 이 둘을 나눠야 장애가 났을 때 어느 데이터를 기준으로 복구할지 정할 수 있습니다.

## 배포본 아키텍처

아래 그림은 `0.2.1` 릴리스에 들어 있던 아키텍처입니다. Snapshot 개발 과정에서 README 구조가 계속 바뀌고 있으므로, 현재 README 그림과 섞지 않고 배포본 자산을 그대로 사용합니다.

[![bluetape4k-javers 0.2.1 아키텍처](../../assets/readme-diagrams/bluetape4k-javers-architecture-01.png)](../../assets/readme-diagrams/bluetape4k-javers-architecture-01.svg)

| 릴리스 모듈 | 맡는 일 | 맡지 않는 일 |
| --- | --- | --- |
| `javers-core` | 코덱, JaVers 확장, 캐시 기반 CDO 저장소 | 업무 데이터 영속 저장 |
| `javers-ddd` | JaVers에 맞춘 저장·커밋·발행 순서 | 생태계 공통 DDD 계약 |
| `javers-exposed` | SQL에 스냅샷과 커밋 메타데이터 저장 | 애플리케이션 CRUD 저장소 |
| `javers-persistence-redis` | Lettuce 또는 Redisson 기반 스냅샷 이력 | 재생 가능한 Kafka 이벤트 로그 |
| `javers-persistence-kafka` | 인코딩한 스냅샷을 Kafka로 발행 | 스냅샷 조회와 관계형 이력 |
| `examples/javers-exposed-ddd` | 명령·감사·이벤트·프로젝션 학습 | 운영용 outbox/트랜잭션 설계 |

일반적인 흐름은 도메인 저장소에서 시작합니다. `AggregateRepository.save()`는 하위 클래스의 `persist`, `javers.commit`, `eventPublisher.publishAll` 순으로 호출합니다. 호출 순서는 고정돼 있지만 실제 전달 시점과 실패 전파 방식은 발행기 구현에 따라 달라집니다. 이 순서는 [`AggregateRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt)에 그대로 드러납니다.

JDBC 트랜잭션과 애플리케이션 저장소는 [bluetape4k-exposed 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/)을, Redis·Kafka·Testcontainers 기반 기능은 [bluetape4k-projects 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/)을 참고하세요.

0.2.1에는 Exposed와 Redis를 함께 쓰거나 Kafka와 조회 저장소를 자동으로 묶는 복합 `CdoSnapshotRepository`가 없습니다. 두 번째 목적지를 추가하기 전에 [저장소 조합](repository-composition.md)을 읽어야 합니다.
