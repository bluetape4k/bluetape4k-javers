# 저장소 조합

SQL에는 오래 보관하고 Redis에서는 빨리 읽고 Kafka로도 발행하고 싶을 수 있습니다. 문제는 0.3.0이 이 세 쓰기를 하나의 원자 작업으로 만드는 복합 스냅샷 저장소를 제공하지 않는다는 점입니다. JaVers에는 `JaversRepository` 하나를 등록하므로 두 번째 목적지부터는 애플리케이션의 일관성 문제입니다.

`javers-core`의 Caffeine, Cache2k, JCache 구현은 각각 완전한 `CdoSnapshotRepository`입니다. GlobalId별 스냅샷을 최신순으로 넣고 커밋 순서는 별도 캐시에 보관합니다. 테스트와 프로세스 내부 이력에는 쓸 수 있지만 재시작하거나 캐시에서 항목이 축출된 뒤에도 데이터가 남는다고 기대하면 안 됩니다. Caffeine 구현은 [`CaffeineCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/caffeine/CaffeineCdoSnapshotRepository.kt)에 있습니다.

`CompositeDomainEventPublisher`는 저장소 조합이 아닙니다. 이벤트 하나를 등록된 발행기 순서대로 넘기며, 중간 발행기가 실패하면 멈춥니다. 앞서 성공한 발행을 되돌리지 않습니다. 정확한 계약은 [`DomainEventPublisher.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/DomainEventPublisher.kt)에 있습니다.

감사 이력을 복구할 기준 저장소 하나를 먼저 고르세요. Kafka는 발행 경로로 추가하고 조회 저장소를 대신하게 만들지 않습니다. Redis 프로젝션을 이벤트로 만들려면 재생, 중복 제거, 오프셋 처리, 불일치 복구까지 서비스가 맡아야 합니다. SQL과 Redis를 직접 함께 쓴다면 부분 실패 뒤 어느 쪽을 기준으로 맞출지와 보정 작업을 정해야 합니다.

[DDD와 CQRS](../guides/ddd-and-cqrs.md)는 0.3.0 예제가 보여 주는 순서와 운영 코드가 보완할 부분을 구분합니다.
