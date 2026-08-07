# 감사 경로 테스트

감사 테스트는 커밋 호출이 끝났다는 사실보다 실제로 끊길 수 있는 경계를 검증해야 합니다.

## 작은 테스트

캐시 기반 저장소로 커밋, 스냅샷, 변경 내역, 섀도를 빠르게 확인합니다. 작성자와 커밋 속성, 최신순 스냅샷, 섀도 복원을 검증하세요. 일부 코덱은 손상된 데이터를 예외 대신 `null`로 처리하므로 깨진 페이로드도 넣어 봐야 합니다. `ShadowProvider`를 쓴다면 JaVers 내부 `typeMapper` 리플렉션이 계속 동작하는지 확인하는 호환성 테스트도 필요합니다.

## 영속 저장 테스트

Exposed에서는 스키마 생성과 마이그레이션으로 시작하는 경우를 나눠 검증합니다. 같은 엔티티를 여러 번 커밋하고 저장소를 다시 만든 뒤 최신 커밋과 최신순 이력을 확인하세요. GlobalId/버전 중복, 스냅샷 저장과 순서 값 갱신 사이의 실패도 다뤄야 합니다. 릴리스의 H2 검증은 [`ExposedCdoSnapshotRepositoryH2Test.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryH2Test.kt)에 있습니다.

Redis는 Lettuce와 Redisson을 각각 Testcontainers에서 실행합니다. 저장소 재생성, 이력, 섀도, 연결 실패, 영속성과 캐시 축출 설정을 확인하세요. Kafka는 발행 키와 페이로드, 30초 실패 경계, 읽기 메서드가 계속 비어 있는지를 검증합니다.

`OrderProjectionFlowTest`는 Kafka와 Redis 컨테이너, H2 도메인·감사 테이블을 사용해 `OrderPlaced`, `OrderMarkedPaid`, Redis 상태를 확인합니다. 정상 순서는 증명하지만 장애 복구, 중복 처리, 오프셋 처리의 정확성, 운영 DB 동작까지 증명하지는 않습니다. Redis와 Kafka의 공유 실행기가 간섭할 수 있으므로 컨테이너 기반 모듈 테스트는 순차 실행합니다.
