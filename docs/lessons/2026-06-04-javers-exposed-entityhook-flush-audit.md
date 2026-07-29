# JaVers Exposed EntityHook flush 감사에서 얻은 교훈

## 배경

JaVers Exposed 통합에서는 모든 저장소가 `javers.commit()`을 명시적으로 호출하게 하는
대신 Exposed flush 수명 주기에서 Exposed DAO 엔티티를 감사할 수 있다.

## 결정

조사 결과를 저장소 로컬 문서로 보존하고 구현은 이슈 #138에서 추적한다. 이 기능을
범용 CDC가 아니라 DAO 전용 `EntityHook` 어댑터로 취급한다.

## 결과

조사 결과, Exposed 1.3.0의 `EntityHook`, `EntityChange`, 트랜잭션 로컬에 등록된 변경
사항과 JaVers의 `commit()` / `commitShallowDeleteById()` API를 사용하는 실현 가능한
경로를 확인했다. 설계에는 재진입 방지, 삭제 의미론, 트랜잭션 경계 테스트 및 가상
스레드에 적합한 JDBC 동작이 포함되어야 한다.

## 검증

- Exposed 1.3.0 소스 jar에서 `EntityHook`, `EntityCache`,
  `EntityLifecycleInterceptor`, `Entity`를 확인함.
- JaVers 7.11.0 소스 jar에서 commit 및 shallow delete API를 확인함.
- 현재 `AggregateRepository`와 `ExposedCdoSnapshotRepository`를 확인함.

## 향후 작업

`javers-exposed` 스냅샷 영속성을 중복 구현하지 않고 #138을 구현한다. 메시지 게시는
훅에서 분리하고 outbox 또는 파이프라인 어댑터 이슈를 통해 처리한다.
