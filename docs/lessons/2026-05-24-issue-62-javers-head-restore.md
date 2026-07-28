# 2026-05-24 — JaVers Redis head 복원

## 배경

Issue #62에서 영속 JaVers 저장소를 기존 Redis 스냅숏으로 다시 구성해도
`headId`가 `null`로 남는 문제가 확인되었다.
`AbstractCdoSnapshotRepository`가 프로세스 로컬 `head` 필드만 반환했기
때문이다. Issue #76은 Redis와 Kafka 저장소의 재시작 및 재구성 시나리오를
검증하도록 요구했다.

## 결정

`AbstractCdoSnapshotRepository`에 지연 실행되는 `restoreHeadId()` 훅을
추가한다. Redis 저장소는 영속화한 `CommitId -> sequence` 메타데이터에서
가장 큰 시퀀스를 선택해 최신 head를 복원한다. Kafka는 명시적으로 쓰기
전용을 유지하므로, 다시 구성한 Kafka 저장소는 head 메타데이터를 복원하지
않는다.

Redisson 시퀀스 맵에서 `readAllMap()`을 사용하려면 키와 값에 서로 다른
코덱이 필요하다. 일반 `LongCodec`은 `StringCodec`으로 키를 인코딩하므로
기존 문자열 commit id 키 바이트와 호환된다. 하지만
`BaseCodec.getMapKeyDecoder()`도 `LongCodec.getValueDecoder()`를 가리켜
`1.00` 같은 키를 Long 값으로 디코딩하려고 한다. 문자열 commit id 키와
long 시퀀스 값에는 `CompositeCodec(StringCodec(), LongCodec())`을 사용한다.

## 결과

이제 Lettuce와 Redisson은 Redis를 비우지 않고 저장소를 다시 구성한 뒤
`headId`를 복원하고, 복원한 head를 사용해 커밋을 이어간다. Kafka에는
지원하지 않는 복원 경로를 명시하는 계약 테스트를 추가했다.

## 검증

- `./gradlew :javers-persistence-redis:test --tests '*LettuceJaversCommitTest*' --tests '*RedissonJaversCommitTest*' --no-daemon` — 6개 통과.
- `./gradlew :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotRepositoryTest*' --no-daemon` — 4개 통과.
- `./gradlew :javers-core:test --no-daemon` — 167개 통과.
- `git diff --check` — 문제 없음.

## 향후 작업 지침

`AbstractCdoSnapshotRepository` 기반 영속 저장소를 추가할 때는
`restoreHeadId()`를 구현할 수 있을 만큼 충분한 커밋 시퀀스 메타데이터를
영속화한다. 쓰기 전용 싱크에는 재시작 후 복원을 지원하지 않는 이유를
명시하는 테스트를 추가한다.
