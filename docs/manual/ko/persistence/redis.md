# Redis 영속 저장

JaVers 스냅샷을 기존 Redis 운영 환경에서 읽고 써야 할 때 선택합니다. Redis라고 해서 곧바로 내구성 있는 이벤트 로그가 되는 것은 아닙니다. 영속성 모드, 복제, 백업, 장애 조치, 캐시 축출 정책에 따라 장애 뒤 이력이 남을지가 달라집니다.

0.3.0은 두 구현을 제공합니다.

- `LettuceCdoSnapshotRepository(name, RedisClient)`는 전용 동기 명령 연결을 씁니다. 스냅샷 목록과 GlobalId 인덱스를 `MULTI/EXEC`로 함께 갱신하고, 동시 쓰기 충돌을 피하려고 쓰기 전용 연결을 사용합니다.
- `RedissonCdoSnapshotRepository(name, RedissonClient)`는 Redisson의 목록 멀티맵과 맵을 사용합니다. 스냅샷 저장과 순서 저장은 하나의 트랜잭션이 아닙니다.

두 구현 모두 GlobalId별 스냅샷을 최신순으로 읽고 커밋 순서를 별도 구조에 보관합니다. 저장소를 다시 만들면 저장된 순서 값에서 최신 커밋을 복원합니다. 구현은 [`LettuceCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt)와 [`RedissonCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt)에 있습니다.

이 매뉴얼은 `0.3.0` 릴리스 소스에 고정돼 있습니다. 해당 릴리스에는 terminal
close guard가 없으므로 아직 초기화하지 않은 lazy connection은 `close()` 이후
operation에서 다시 열릴 수 있습니다. terminal lifecycle 계약은 `0.3.0` 이후에
수정됐으므로 `0.4.0` 개발선을 사용할 때는 current module README를 확인하세요.

## 선택과 복구

서비스가 Lettuce 클라이언트의 수명을 이미 관리한다면 Lettuce를, Redisson 분산 객체가 운영 표준이라면 Redisson을 고르세요. 두 구현의 저장 구조가 같다고 가정해 같은 네임스페이스를 공유하면 안 됩니다.

Redis 오류는 호출자에게 전달되지만 일부만 갱신될 가능성은 남습니다. 디코딩에 실패한 값은 조회에서 빠질 수 있고, 넓은 JQL 조회는 키와 스냅샷을 프로세스 메모리에 올립니다. 커밋 ID와 스냅샷 버전으로 재시도 결과를 확인하고 키 수, 메모리, 조회 지연을 함께 감시하세요. 클라이언트와 토폴로지는 [Projects Redis 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-redis/)로 이어집니다.
