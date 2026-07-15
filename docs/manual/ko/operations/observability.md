# 관측

API 호출이 성공해도 업무 상태, 감사 이력, projection이 계속 맞는다는 뜻은 아닙니다. 경계마다 신호를 따로 모으고 같은 식별자로 연결해야 합니다.

## 수집할 신호

- 저장소 종류와 aggregate type별 JaVers 커밋 지연·실패
- 코덱 encode/decode 실패, 커밋 ID, GlobalId, 스냅샷 버전
- Exposed 트랜잭션 오류, unique 충돌, table 증가량, 넓은 조회 지연
- Redis 명령 지연, 키 수, 메모리·eviction, persistence 상태, replication lag
- Kafka send 지연·timeout, producer 오류, topic/partition, 소비자 lag, 재시도/dead-letter, 프로젝션 실패
- 정기 reconciliation에서 찾은 domain-to-audit, domain-to-프로젝션 불일치 수

`AbstractCdoSnapshotRepository.getAll()`은 모든 key와 snapshot을 읽고 정렬한 뒤 조건을 적용합니다. key가 10,000개를 넘었다는 경고는 단순 로그 소음이 아니라 조회 방식 재검토 신호입니다. Kafka repository의 읽기 메서드도 호출될 때마다 지원하지 않는 경로라는 경고를 남깁니다. 통합 테스트에서 이 경고를 잡으면 저장 역할을 잘못 고른 경우를 빨리 찾을 수 있습니다.

## Drift 확인

애플리케이션 aggregate ID 하나를 골라 최신 audit snapshot과 예상 업무 버전 또는 필드를 비교합니다. 이어서 Redis의 최신 이벤트/프로젝션 version을 비교합니다. 차이가 나면 domain-to-audit, audit-to-publication, 소비자 lag, 프로젝션 apply 중 어디서 끊겼는지 분류하세요. 민감한 전체 payload를 메트릭 label에 넣지 말고 상세 증거는 접근 통제된 진단 로그에 남깁니다.

예제 consumer는 poll 순서대로 적용하고 같은 key의 Kafka 순서를 기대하지만 lag metric과 offset 관리는 제공하지 않습니다. 운영 구성에서 추가해야 합니다. 소스는 [`OrderProjectionEventConsumer.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/main/kotlin/io/bluetape4k/javers/examples/exposedddd/messaging/OrderProjectionEventConsumer.kt)입니다.
