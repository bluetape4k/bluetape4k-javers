# Issue #211 Kafka 프로젝션 head 및 시퀀스 의미

## 배경

`KafkaCdoSnapshotProjector`는 Kafka 레코드를 디코딩한 뒤
`saveSnapshot(snapshot)`을 직접 호출했다. 이 방식은 스냅샷 행은 재구성하지만
커밋 head와 시퀀스 메타데이터를 관리하는 저장소 경로를 우회한다. 그 결과
재생된 저장소에서 데이터를 읽을 수는 있어도 오래되었거나 누락된 head 상태가
보고될 수 있었다.

## 결정

재생 계약으로 `CdoSnapshotRepository.projectSnapshot`을 추가한다. 단순한 사용자
정의 저장소를 위해 기본 메서드는 스냅샷만 처리하고,
`AbstractCdoSnapshotRepository`는 스냅샷 데이터, 커밋 시퀀스, head 상태를
복원한다. Exposed와 Lettuce는 프로젝션 영속화 훅을 재정의하여 스냅샷과
시퀀스 갱신이 백엔드 트랜잭션 또는 `MULTI`/`EXEC` 경계를 공유하도록 한다.

## 결과

이제 Kafka 프로젝션은 core, Exposed, Lettuce Redis 프로젝션 대상에서
`getHeadId()`와 조회 순서를 복원한다. 오프셋 커밋은 프로젝션 성공 후에만
수행되며, 디코딩 또는 프로젝션에 실패하면 이전과 마찬가지로 오프셋을
커밋하지 않는다.

## 향후 지침

프로젝터가 디코딩된 감사 레코드를 재생할 때는 `saveSnapshot`이 아니라
`projectSnapshot`을 호출해야 한다. 새로운 내구성 저장소는
`AbstractCdoSnapshotRepository`를 상속하거나, 동등한 head 및 시퀀스 복원
의미를 갖도록 `projectSnapshot`을 재정의해야 한다.
