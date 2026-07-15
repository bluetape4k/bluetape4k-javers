# 학습 경로

맡은 역할에 따라 필요한 순서만 골라 읽을 수 있습니다.

## 애플리케이션 개발자

[시작하기](../getting-started.md)에서 영속 저장소 하나를 구성합니다. 이어서 [감사 모델](../architecture/audit-model.md)을 읽으면 snapshot과 업무 row, shadow와 현재 entity를 구분할 수 있습니다. 마지막으로 [DDD와 CQRS](ddd-and-cqrs.md)에서 업무 저장, 감사 커밋, 이벤트 발행, 프로젝션 갱신 순서와 운영용 outbox가 메워야 할 틈을 확인합니다.

## 영속 연동 담당자

[저장소 지도](../architecture/repository-map.md)와 [영속 방식 선택](../persistence/selection-guide.md)을 먼저 읽으세요. Exposed 문서는 schema와 트랜잭션 책임을, Redis 문서는 Lettuce와 Redisson 차이를, Kafka 문서는 write-only 계약을 설명합니다. [실패 계약](../operations/failure-contracts.md)까지 읽은 뒤 두 번째 목적지를 연결해야 재시도 기준을 정할 수 있습니다.

## 운영자와 테스트 담당자

[관측](../operations/observability.md)에서 커밋 실패, 넓은 조회 비용, Kafka lag, 프로젝션 불일치 신호를 고릅니다. [테스트](testing.md)는 이 위험을 unit, DB, Testcontainers 검증으로 나눕니다. 전체 흐름의 실행 기준은 [`OrderProjectionFlowTest`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/OrderProjectionFlowTest.kt)입니다.

공통 기반은 [Projects 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/)과 [Exposed 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/)로 이어집니다.
