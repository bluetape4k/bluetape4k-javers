# 저장소 지도

`bluetape4k-javers`는 JaVers 감사와 이력의 의미를 맡습니다. 현재 업무 상태를 저장하는 애플리케이션 repository까지 대신하지는 않습니다. 이 둘을 나눠야 장애가 났을 때 어느 데이터를 기준으로 복구할지 정할 수 있습니다.

| 릴리스 모듈 | 맡는 일 | 맡지 않는 일 |
| --- | --- | --- |
| `javers-core` | 코덱, JaVers 확장, 캐시-backed CDO 저장소 | 업무 데이터 영속 저장 |
| `javers-ddd` | JaVers에 맞춘 저장·커밋·발행 순서 | 생태계 공통 DDD 계약 |
| `javers-exposed` | SQL에 snapshot과 커밋 metadata 저장 | 애플리케이션 CRUD 저장소 |
| `javers-persistence-redis` | Lettuce 또는 Redisson 기반 스냅샷 이력 | 재생 가능한 Kafka 이벤트 log |
| `javers-persistence-kafka` | encoded snapshot을 Kafka로 발행 | 스냅샷 조회와 관계형 이력 |
| `examples/javers-exposed-ddd` | 명령·감사·이벤트·프로젝션 학습 | 운영용 outbox/트랜잭션 설계 |

일반적인 흐름은 도메인 repository에서 시작합니다. `AggregateRepository.save()`는 하위 클래스의 `persist`, `javers.commit`, 동기 발행기 순으로 호출합니다. 이 순서는 [`AggregateRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt)에 그대로 드러납니다.

JDBC transaction과 애플리케이션 repository는 [bluetape4k-exposed 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/)을, Redis·Kafka·Testcontainers 기반 기능은 [bluetape4k-projects 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/)을 참고하세요.

0.2.1에는 Exposed와 Redis를 함께 쓰거나 Kafka와 조회 저장소를 자동으로 묶는 composite `CdoSnapshotRepository`가 없습니다. 두 번째 목적지를 추가하기 전에 [저장소 조합](repository-composition.md)을 읽어야 합니다.
