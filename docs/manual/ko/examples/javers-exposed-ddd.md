# JaVers + Exposed DDD 주문 흐름

이 예제는 주문 하나가 Exposed 명령 저장소, JaVers 감사 저장소, Kafka 도메인 이벤트, Redis 조회 모델을 차례로 거치는 과정을 보여 줍니다. 코드 양보다 경계가 중요합니다. 현재 상태, 감사 이력, 이벤트 전달, 조회 프로젝션을 서로 다른 책임으로 나눠 놓았기 때문에 각 단계의 성공과 실패를 따로 검증할 수 있습니다.

## 예제가 풀려는 문제

`PlaceOrderCommand`와 `MarkOrderPaidCommand`는 `Order` 애그리거트의 상태를 바꿉니다. 이때 애플리케이션은 네 가지 질문에 답해야 합니다.

1. 현재 주문 상태는 무엇인가?
2. 누가 상태를 바꿨고 JaVers에는 무엇이 남았는가?
3. 명령 처리 후 어떤 도메인 이벤트가 발행됐는가?
4. 조회 측은 애그리거트를 다시 읽지 않고 무엇을 보여 줄 수 있는가?

예제는 이 책임을 저장소별로 나눕니다. Exposed가 현재 `Order`를 저장하고, `ExposedCdoSnapshotRepository`가 JaVers 커밋과 스냅샷을 저장합니다. Kafka는 `OrderPlaced`, `OrderMarkedPaid`를 전달하고, Redis는 `OrderSummary`를 보관합니다. Redis는 조회용 프로젝션이지 원본 저장소가 아닙니다. JaVers 테이블도 감사 저장소이지 명령 측 주문 테이블을 대신하지 않습니다.

## 0.3.0 테스트가 준비하는 환경

JDK 21과 Docker 호환 컨테이너 실행 환경을 준비한 뒤 저장소 루트에서 실행합니다.

```bash
./gradlew :javers-exposed-ddd:test
```

0.3.0 테스트는 PostgreSQL 서버를 띄우지 않습니다. `OrderCommandHandlerTest`와 `OrderProjectionFlowTest`는 `MODE=PostgreSQL`을 켠 인메모리 H2를 사용합니다. 프로젝션 테스트는 Projects에서 제공하는 Testcontainers 실행 도구로 Kafka와 Redis를 시작합니다.

| 책임 | 0.3.0 테스트 환경 | 검증하는 범위 |
|---|---|---|
| 주문·JaVers 테이블 | PostgreSQL 호환 모드의 H2 | 예제 스키마에서 명령, 스냅샷, 이력 조회가 이어지는지 |
| 도메인 이벤트 전달 | Kafka Testcontainer | 주문 ID를 키로 이벤트를 발행하고 소비하는지 |
| 조회 프로젝션 | Redis Testcontainer | 소비한 이벤트로 `OrderSummary`를 만들고 읽을 수 있는지 |

운영에 적용하기 전에는 실제 PostgreSQL Testcontainer로 한 번 더 검증해야 합니다. H2 호환 모드만으로 PostgreSQL DDL, 잠금, 격리 수준, 실행 계획, JDBC 드라이버 동작까지 확인했다고 볼 수 없습니다. 애플리케이션 저장소와 트랜잭션 경계는 [Exposed 트랜잭션 소유권 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/1.11/modules/bluetape4k-exposed-jdbc/transaction-ownership/)을, PostgreSQL·Kafka·Redis·Testcontainers 기반 기능은 [Projects 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/)을 참고하세요.

## 명령 처리 흐름 읽기

다음 순서로 읽으면 명령 측 책임이 한 단계씩 보입니다.

1. [`Order.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/Order.kt)에서 애그리거트 불변 조건과 `PLACED`에서 `PAID`로 바뀌는 규칙을 확인합니다.
2. [`OrderCommand.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/OrderCommand.kt)는 요청 의도와 저장 상태를 분리합니다.
3. [`OrderCommandHandler.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/service/OrderCommandHandler.kt)는 새 애그리거트를 만들거나 기존 애그리거트를 읽어 상태를 바꿉니다.
4. [`OrderRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/persistence/OrderRepository.kt)는 Exposed로 `example_order`를 저장한 뒤 `AggregateRepository`에 JaVers 커밋과 이벤트 발행을 맡깁니다.
5. [`OrderCommandHandlerTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/OrderCommandHandlerTest.kt)에서 명령 처리 결과를 확인합니다.

단가가 `12.50`인 상품 두 개로 `PlaceOrderCommand(order-1)`을 실행하면 테스트는 다음 결과를 기대합니다.

- 원본 주문의 상태는 `PLACED`, 합계는 `25.00`입니다.
- JaVers 스냅샷은 하나이며 `domainEventType`은 `OrderPlaced`입니다.
- 발행된 이벤트는 하나이며 고객, 합계, 애그리거트 ID, 고정된 발생 시각을 담습니다.

이어서 `MarkOrderPaidCommand`를 실행하는 테스트는 현재 상태 `PAID`, 감사 스냅샷 두 개, `OrderPlaced` 다음 `OrderMarkedPaid` 순서의 발행 결과를 검증합니다.

애그리거트나 감사 동작을 바꿨다면 명령 측 테스트만 좁혀서 실행할 수 있습니다.

```bash
./gradlew :javers-exposed-ddd:test --tests '*OrderCommandHandlerTest*'
```

## Kafka 이벤트에서 Redis 조회 모델까지

이제 도메인 이벤트가 조회 결과로 바뀌는 경로를 읽습니다.

1. [`OrderEvents.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/domain/OrderEvents.kt)는 감사 속성과 이벤트 내용을 정의합니다.
2. [`OrderDomainEventJsonCodec.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderDomainEventJsonCodec.kt)는 이 예제에서만 쓰는 JSON 형식을 고정합니다.
3. [`OrderKafkaEventPublisher.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderKafkaEventPublisher.kt)는 주문 ID를 레코드 키로 삼아 동기식으로 전송합니다.
4. [`OrderProjectionEventConsumer.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderProjectionEventConsumer.kt)는 가져온 순서대로 이벤트를 적용합니다.
5. [`RedisOrderSummaryProjection.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/projection/RedisOrderSummaryProjection.kt)는 주문별 JSON 문서 하나를 Redis에 저장합니다.
6. [`OrderQueryService.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/service/OrderQueryService.kt)는 Redis만 읽습니다.

0.3.0의 프로젝션 통합 테스트는 다음 명령으로 실행합니다.

```bash
./gradlew :javers-exposed-ddd:test --tests '*OrderProjectionFlowTest*'
```

테스트는 단가가 `15.00`인 상품 두 개를 주문한 뒤 Redis에 고객 `customer-projection-1`, 합계 `30.00`, 상태 `PLACED`인 `OrderSummary`가 생길 때까지 Kafka를 폴링합니다. 결제 명령을 처리한 뒤에는 같은 Redis 키의 상태가 `PAID`로 바뀌고, 갱신 시각이 고정된 `Clock` 값과 같은지 확인합니다. 이 폴링 반복문은 테스트 검증을 위한 장치입니다. 운영 소비자 반복문의 구현 예시는 아닙니다.

## 모듈을 따라 배우는 순서

예제 전체를 한꺼번에 읽기보다 다음 순서로 책임을 하나씩 추가하세요.

1. [javers-core](../modules/javers-core.md): 영속 연동 없이 객체를 커밋하고 스냅샷을 조회합니다.
2. [javers-exposed](../modules/javers-exposed.md): 인메모리 JaVers 저장소를 Exposed 기반 커밋·스냅샷 테이블로 바꿉니다.
3. [javers-ddd](../modules/javers-ddd.md): 애그리거트 저장, 감사 메타데이터, 상태 변경 뒤 동기식 `DomainEventPublisher`를 추가합니다.
4. Kafka 이벤트: `OrderPlaced`나 `OrderMarkedPaid`를 직렬화하고 안정적인 주문 ID 키로 발행합니다.
5. Redis 프로젝션: 이벤트를 `OrderSummary`에 반영하고 명령 측 테이블과 독립적으로 조회합니다.

이 순서로 읽으면 각 계층의 책임과 실패 지점이 섞이지 않습니다. [DDD와 CQRS](../guides/ddd-and-cqrs.md), [실패 계약](../operations/failure-contracts.md), [벤치마크 해석](../benchmarks/exposed-ddd-envers.md)도 함께 보세요.

## 운영에 그대로 가져가면 안 되는 부분

0.3.0 예제는 여러 자원의 조정을 애플리케이션 책임으로 남겨 둡니다.

- `OrderRepository.persist`, JaVers 커밋, Kafka 발행은 순차 작업 세 개이며 하나의 트랜잭션이 아닙니다.
- JaVers 커밋이 실패하면 주문 행만 저장될 수 있습니다.
- Kafka 발행이 실패하면 주문과 감사 이력은 남았지만 이벤트는 없을 수 있습니다.
- 트랜잭셔널 아웃박스와 인박스, 재시도 큐, 처리 불가 메시지 보관, 이벤트 ID, 중복 제거 저장소, 재처리 진행 위치가 없습니다.
- Kafka 순서는 같은 주문 ID가 같은 파티션으로 간 레코드에 대해서만 기대할 수 있습니다. 토픽과 프로듀서 설정도 영향을 줍니다.
- `OrderProjectionEventConsumer`는 운영 서비스에 필요한 오프셋 관리, 재시도, 처리할 수 없는 레코드의 격리, 리밸런싱, 정상 종료를 다루지 않습니다.
- `OrderMarkedPaid`는 Redis에 기존 요약 정보가 있어야 합니다. 이벤트가 빠지거나 중복되거나 순서가 바뀌면 프로젝션이 실패하거나 잘못될 수 있습니다.
- JSON 코덱에는 스키마 버전과 호환성 규칙이 없습니다.
- Redis에는 최신 조회 결과만 있습니다. 주문 테이블이나 JaVers 이력을 대신하지 않습니다.
- 실제 PostgreSQL, 프로세스 재시작, 브로커 장애, 동시 명령, 프로젝션 재구축은 테스트하지 않습니다.

운영에서는 주문과 같은 데이터베이스 트랜잭션에 아웃박스를 저장하고, 소비자의 멱등성 기준을 정해야 합니다. 이벤트 스키마에 버전을 부여하고 재처리 진행 위치를 보관하며, 내구성 있는 이벤트 저장소나 감사 원본에서 프로젝션을 재구축할 절차도 필요합니다. 명령 측 세 단계는 각각 관측하세요.

## 경계를 확인하는 실습

1. H2를 PostgreSQL Testcontainer로 바꾸고 스키마 생성, 타임스탬프, 동시 갱신을 검증합니다.
2. `example_order` 커밋 직후 JaVers 저장소를 실패시키고, 누락된 감사 버전을 정합성 점검으로 찾습니다.
3. Kafka 발행을 실패시킨 뒤 주문과 같은 데이터베이스 트랜잭션에 아웃박스 행을 저장하고 나중에 발행합니다.
4. 이벤트 ID를 추가하고 중복 전달에도 Redis 프로젝션 결과가 같도록 만듭니다.
5. `OrderMarkedPaid`를 `OrderPlaced`보다 먼저 전달한 뒤 재시도, 격리, 재구축 중 하나를 선택합니다.
6. Redis 키를 지운 뒤 명령 측 테이블을 직접 읽지 않고 프로젝션을 복구합니다.
7. 이벤트 JSON에 버전을 넣고 이전 형식의 내용도 읽는 코덱 호환성 테스트를 작성합니다.

0.3.0 예제의 전체 의존성은 [`build.gradle.kts`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/examples/javers-exposed-ddd/build.gradle.kts)에 고정돼 있습니다. 이후 저장소의 구현과 비교하더라도 이 매뉴얼의 설명은 해당 릴리스 경계를 기준으로 읽어야 합니다.
