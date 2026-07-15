# Redis 영속 저장

JaVers 스냅샷을 기존 Redis 운영 환경에서 읽고 써야 할 때 선택합니다. Redis라고 해서 곧바로 내구성 있는 이벤트 로그가 되는 것은 아닙니다. persistence mode, replication, backup, failover, eviction 정책이 장애 뒤 이력이 남는지를 결정합니다.

0.2.1은 두 구현을 제공합니다.

- `LettuceCdoSnapshotRepository(name, RedisClient)`는 전용 동기 명령을 씁니다. 스냅샷 목록과 GlobalId 인덱스를 MULTI/EXEC로 함께 갱신하고, 동시 쓰기 충돌을 피하려고 전용 write connection을 사용합니다.
- `RedissonCdoSnapshotRepository(name, RedissonClient)`는 Redisson list-multimap과 map을 사용합니다. 스냅샷 저장과 순서 저장은 하나의 트랜잭션이 아닙니다.

둘 다 GlobalId별 스냅샷을 최신순으로 읽고 커밋 순서를 별도 구조에 보관합니다. 저장소를 다시 만들어도 저장된 순서 값에서 head를 찾습니다. 구현은 [`LettuceCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/LettuceCdoSnapshotRepository.kt)와 [`RedissonCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-persistence-redis/src/main/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedissonCdoSnapshotRepository.kt)에 있습니다.

서비스가 Lettuce client 생명주기를 이미 관리한다면 Lettuce를, Redisson distributed object가 운영 표준이면 Redisson을 고르세요. 두 구현의 wire 구조가 같다고 가정해 같은 namespace를 공유하면 안 됩니다.

Redis 오류는 호출자에게 전달되지만 부분 갱신 가능성은 남습니다. decode에 실패한 값은 조회에서 빠질 수 있고, 넓은 JQL 조회는 키와 스냅샷을 프로세스 메모리에 올립니다. 커밋 ID와 스냅샷 버전으로 재시도 결과를 확인하고 키 수, 메모리, 조회 지연을 함께 감시하세요. client와 topology는 [Projects Redis 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/1.11/modules/bluetape4k-redis/)로 이어집니다.
