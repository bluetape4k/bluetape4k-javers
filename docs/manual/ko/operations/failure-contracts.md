# 실패 계약

0.3.0 어댑터는 저장과 발행 오류를 호출자에게 전달합니다. 오류 전달이 앞선 쓰기의 롤백까지 뜻하지는 않습니다.

| 실패 지점 | 남을 수 있는 상태 | 운영 코드가 할 일 |
| --- | --- | --- |
| 업무 저장 | 감사와 이벤트 없음 | 명령 재시도 정책 적용 |
| JaVers 스냅샷/순서 | 업무 상태만 있거나 감사 일부만 저장 | 애그리거트, GlobalId, 버전, 커밋 ID로 보정 |
| Kafka 발행 | 업무와 감사는 있지만 이벤트 없음 | outbox/재시도 기록, 명령 전체 재실행 금지 |
| 소비자/프로젝션 | 레코드 재전달 가능, Redis 지연 또는 부분 반영 | 멱등 적용, 오프셋 통제, 재생과 불일치 복구 |

`AbstractCdoSnapshotRepository.persist`는 스냅샷을 하나씩 저장한 뒤 메모리의 최신 커밋과 커밋 순서를 갱신합니다. 중간 오류가 나면 다음 작업은 멈추지만 이미 끝난 외부 쓰기는 취소하지 않습니다. 순서 저장이 실패하면 같은 인스턴스와 다시 만든 저장소가 기억하는 최신 커밋이 달라질 수도 있습니다. 구현은 [`AbstractCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt)에 있습니다.

멱등성은 애플리케이션 계약입니다. SQL 고유 인덱스, Kafka GlobalId 키, 예제의 Redis 고정 키가 있더라도 명령과 이벤트 중복 제거가 완성되는 것은 아닙니다. 안정적인 명령/이벤트 ID를 기록하고 중복 커밋 허용 여부를 정한 뒤 프로젝션이 이벤트 순서나 버전을 비교하게 만드세요.

스키마도 배포 절차가 소유합니다. `ensureSchema()`는 마이그레이션 이력을 관리하지 않습니다. Kafka 토픽 보존 기간과 파티션, Redis 영속성과 축출 정책도 같은 수준의 정확성 설정입니다. 소유자와 복구 명령을 서비스 운영 문서에 함께 적어야 합니다.
