# 실패 계약

0.2.1 adapter는 저장과 발행 오류를 호출자에게 전달합니다. 오류 전달이 앞선 쓰기의 rollback까지 뜻하지는 않습니다.

| 실패 지점 | 남을 수 있는 상태 | 운영 코드가 할 일 |
| --- | --- | --- |
| 업무 저장 | 감사와 이벤트 없음 | 명령 재시도 정책 적용 |
| JaVers 스냅샷/sequence | 업무 상태만 있거나 감사 일부만 저장 | aggregate, GlobalId, 버전, 커밋 ID로 보정 |
| Kafka 발행 | 업무와 감사는 있지만 이벤트 없음 | outbox/재시도 기록, 명령 전체 재실행 금지 |
| 소비자/프로젝션 | 레코드 재전달 가능, Redis 지연 또는 부분 반영 | 멱등 적용, offset 통제, replay와 불일치 복구 |

`AbstractCdoSnapshotRepository.persist`는 snapshot을 하나씩 저장한 뒤 메모리 head와 커밋 sequence를 갱신합니다. 중간 오류가 나면 다음 작업은 멈추지만 이미 끝난 외부 쓰기는 취소하지 않습니다. sequence 저장이 실패하면 같은 인스턴스의 head와 재생성한 repository의 head가 달라질 수도 있습니다. 구현은 [`AbstractCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt)에 있습니다.

멱등성은 애플리케이션 계약입니다. SQL unique index, Kafka GlobalId 키, 예제의 Redis 고정 key가 있더라도 command와 이벤트 중복 제거가 완성되는 것은 아닙니다. 안정적인 명령/이벤트 ID를 기록하고 duplicate 커밋 허용 여부를 정한 뒤 projection이 이벤트 sequence나 version을 비교하게 만드세요.

schema도 배포 절차가 소유합니다. `ensureSchema()`는 migration 이력을 관리하지 않습니다. Kafka topic retention과 partition, Redis persistence와 eviction도 같은 수준의 정확성 설정입니다. 소유자와 복구 명령을 서비스 운영 문서에 함께 적어야 합니다.
