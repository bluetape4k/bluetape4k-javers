# Issue #138 Exposed EntityHook 감사 작업에서 얻은 교훈

## 배경

`javers-exposed`에는 모든 DAO 쓰기 메서드에서 `javers.commit()`을 명시적으로
호출하지 않아도 되는 DAO 생명주기 감사 경로가 필요했다.

## 결정

이 기능은 엄격한 `EntityClass` 매핑, 분리된 감사 객체 매퍼, ID 기준 최종 삭제
스냅샷, `AutoCloseable` 구독 해제 의미 체계를 갖춘 명시적 `EntityHook` 구독으로
구현한다.

## 결과

어댑터는 DAO 전용으로 유지하며 CDC, 원시 DSL 쓰기, 발행 파이프라인은 범위에서
제외한다. 테스트는 생성, 수정, 삭제, 롤백, 단일 트랜잭션의 최종 상태, 구독 해제
동작을 검증한다.

## 검증

- Exposed 1.3.0 소스 JAR에서 `EntityHook`, `registeredChanges()`,
  `transactionScope`를 확인했다.
- JaVers 7.11.0 소스 JAR에서 `commit()`과 `commitShallowDeleteById()`를
  확인했다.

## 향후 유의 사항

`EntityHook`이 원시 Exposed DSL이나 CDC까지 지원한다고 주장해서는 안 된다. 발행
의미 체계가 필요하다면 별도의 아웃박스 또는 파이프라인 어댑터를 추가한다.
