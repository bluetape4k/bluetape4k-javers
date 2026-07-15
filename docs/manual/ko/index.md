# bluetape4k-javers 0.2 매뉴얼

애플리케이션의 현재 상태, 감사 이력, 조회용 projection을 한 저장소의 역할로 묶으면 장애 복구 기준이 흐려집니다. `bluetape4k-javers` 0.2.1은 Exposed, Redis, Kafka를 JaVers에 연결하지만 세 방식이 맡는 일은 서로 다릅니다. 이 매뉴얼은 기능 목록보다 먼저 그 경계를 설명합니다.

설명 기준은 `0.2.1` 릴리스와 커밋 `bffe19439ca891fa5301a76421bdef7ba75252a0`입니다. Ktor 연동, Spring Boot 4 자동 구성과 예제, 전용 Gradle 벤치마크 모듈은 0.2.1 뒤에 추가됐으므로 0.2 기능으로 다루지 않습니다.

## 어디서 시작할까

- [시작하기](getting-started.md): 생태계 버전 하나로 의존성을 맞추고 Exposed 기반 감사 저장소를 구성합니다.
- [저장소 지도](architecture/repository-map.md): 모듈마다 맡는 책임과 맡지 않는 책임을 구분합니다.
- [영속 방식 선택](persistence/selection-guide.md): 복구와 조회 요구를 기준으로 Exposed, Redis, Kafka를 고릅니다.
- [학습 경로](guides/learning-path.md): 개발자, 연동 담당자, 운영자에게 맞는 읽기 순서를 안내합니다.

JaVers 데이터 구조가 먼저 궁금하면 [감사 모델](architecture/audit-model.md)을 읽으세요. 여러 저장 경로를 엮는다면 [저장소 조합](architecture/repository-composition.md)과 [실패 계약](operations/failure-contracts.md)을 먼저 확인해야 합니다. command에서 Redis projection까지 이어지는 흐름은 [DDD와 CQRS](guides/ddd-and-cqrs.md)에 있습니다.

동작 기준은 릴리스 소스입니다. 출발점은 [`CdoSnapshotRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepository.kt), [`AggregateRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt), [`javers-exposed-ddd` 예제](https://github.com/bluetape4k/bluetape4k-javers/tree/0.2.1/examples/javers-exposed-ddd)입니다.
