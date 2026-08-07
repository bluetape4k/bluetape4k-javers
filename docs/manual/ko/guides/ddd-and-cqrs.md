# DDD와 CQRS

0.3.0 예제는 명령을 처리할 때 업무 상태, 감사 이력, 이벤트, 조회 프로젝션이 어떤 순서로 움직이는지 보여 줍니다.

[![DDD와 CQRS 주문 처리 순서](../../assets/examples/ddd-cqrs-sequence.png)](../../assets/examples/ddd-cqrs-sequence.svg)

`AggregateRepository.save`는 다음 순서로 실행됩니다.

1. 하위 클래스의 `persist(aggregate)`가 업무 원본을 저장합니다.
2. `javers.commit(author, saved, eventProperties)`가 감사 커밋을 남깁니다.
3. `DomainEventPublisher.publishAll`이 이벤트를 순서대로 발행합니다.
4. Kafka 소비자가 나중에 이벤트를 읽어 `RedisOrderSummaryProjection`에 적용합니다.

근거는 [`AggregateRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt), [`OrderRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/persistence/OrderRepository.kt), [`OrderProjectionFlowTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/OrderProjectionFlowTest.kt)입니다.

예제가 증명하는 것은 책임과 정상 순서입니다. DB 쓰기와 Kafka 전송을 하나의 트랜잭션으로 만들지는 않습니다. 업무 저장 뒤 감사 커밋이 실패하면 현재 상태만 남을 수 있습니다. 발행이 실패하면 업무와 감사는 남지만 프로젝션 이벤트가 없습니다. 소비자가 Redis를 바꾼 뒤 오프셋 처리 전에 실패하면 같은 이벤트를 다시 적용할 수 있습니다. `OrderMarkedPaid`가 먼저 오거나 `OrderPlaced`가 빠지면 요약 정보를 찾지 못해 실패합니다.

운영 시스템은 트랜잭셔널 아웃박스 또는 동등한 복구 기록, 안정적인 이벤트 ID, 멱등 프로젝션, 오프셋 커밋 정책, 재시도와 데드 레터 처리, 재생 도구, 정합성 점검을 추가해야 합니다. `javers-ddd`는 JaVers 감사 흐름을 돕는 모듈이지 bluetape4k 전체의 일반 DDD 계약 소유자가 아닙니다.
