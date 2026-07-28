# 이슈 89 Kafka-Redis 프로젝션 교훈

## 배경

#89는 병합된 #88 명령 측 예제에 이어 상위 이슈 #5의 조회 측 CQRS
프로젝션을 추가한다.

## 결정

예제 런타임은 가볍게 유지한다. 일반 Kafka 클라이언트로 주문 이벤트의 JSON
봉투를 발행하고 소비하며, Lettuce는 `OrderSummary`마다 Redis JSON 문서 하나를
저장한다. 이번 작업에서는 Spring Boot HTTP 연결을 추가하지 않고 조회 측을
`OrderQueryService`로 노출한다.

## 결과

명령 처리기는 `OrderPlaced`와 `OrderMarkedPaid` 이벤트를 Kafka에 발행할 수
있다. 프로젝션 소비자는 Redis를 갱신하고, 조회 API는 주문 접수 및 결제 완료
요약을 반환한다.

## 검증

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  실행 결과 Kafka 및 Redis Testcontainers 흐름을 포함한 테스트 3개가 통과했다.

## 향후 지침

#90은 새로 측정한 벤치마크 근거에 집중한다. Envers 비교나 운영 환경의 아웃박스
보장을 이번 프로젝션 작업 범위에 다시 포함하지 않는다.
