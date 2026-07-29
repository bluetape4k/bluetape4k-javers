# Issue #138 — Exposed EntityHook JaVers Audit 설계

## 목표

`javers-exposed`에 Exposed DAO `EntityHook` 기반 JaVers audit adapter를 추가한다.
DAO entity 생성, 수정, 삭제가 Exposed flush lifecycle에서 감지되면 같은 Exposed
transaction 안에서 JaVers snapshot을 저장한다.

## 근거

- GitHub issue #138: Exposed DAO `EntityHook` audit adapter 구현.
- `develop` 현재 HEAD: `5266171 refactor: default snapshot query predicate`.
- 선행 이슈 #116, #115, #106, #103은 병합되어 codec contract, transaction event,
  schema option, SQL pushdown 기반이 준비되었다.
- Exposed 1.3.0 source jar 확인:
  - `EntityHook.subscribe()` / `unsubscribe()`는 전역 subscriber를 등록한다.
  - `EntityChangeType`은 `Created`, `Updated`, `Removed`를 제공한다.
  - `Transaction.registeredChanges()`는 transaction-local lifecycle event 전체를 반환한다.
  - `EntityLifecycleInterceptor.beforeCommit()`은 entity flush 후 subscriber를 호출한다.
  - `transactionScope`는 transaction user data에 scoped state를 저장할 수 있다.
- JaVers 7.11.0 source jar 확인:
  - 생성/수정은 `commit(author, object, properties)`로 저장한다.
  - 삭제는 `commitShallowDeleteById(author, GlobalIdDTO, properties)`로 terminal snapshot을 저장한다.

## 범위

### Public API

- `ExposedJaversEntityHookMapping`
  - 하나의 Exposed DAO `EntityClass`를 하나의 JaVers audit type에 매핑한다.
  - 생성/수정 mapper는 flushed DAO entity를 detached audit object로 변환한다.
  - 삭제 mapper는 `EntityID`에서 JaVers local id를 추출한다.
- `ExposedJaversEntityHookSubscription`
  - `AutoCloseable` lifecycle object.
  - 생성 시 `EntityHook`에 subscriber를 등록하고 `close()`에서 해제한다.
  - `subscribe(javers, mappings, ...)` companion factory를 제공한다.

### 동작 계약

- DAO lifecycle 전용이다. Exposed DSL `insert`, `update`, `deleteWhere`, external DB write,
  CDC, outbox publication은 감지한다고 주장하지 않는다.
- configured `EntityClass`에 매칭되는 event만 처리한다.
- 같은 flush batch 안에서 동일 entity에 여러 event가 등록되면
  `registeredChanges()`의 마지막 event만 JaVers에 반영한다.
- `Created` / `Updated`:
  - `EntityChange.toEntity(mapping.entityClass)`로 flushed entity를 다시 읽는다.
  - entity가 없으면 무시한다.
  - mapper가 반환한 detached audit object를 `javers.commit()`에 전달한다.
- `Removed`:
  - 삭제 후 DAO entity를 다시 읽을 수 없으므로 `EntityID`와 audit type으로
    `InstanceIdDTO.instanceId(localId, auditType)`를 만든다.
  - `javers.commitShallowDeleteById()`로 terminal snapshot을 저장한다.
- author와 commit properties는 callback으로 제공한다.
- JaVers repository write가 hook을 재진입시키지 않도록 transaction-local guard를 둔다.
- `ExposedCdoSnapshotRepository(database)`가 현재 Exposed transaction을 재사용하는지
  생성/수정/삭제/rollback 테스트로 검증한다.

## 비범위

- Spring Boot auto-configuration.
- raw Exposed DSL audit 또는 database CDC.
- Kafka/Redis/NATS/SQS publication.
- delete-before full object snapshot.
- cross-transaction buffering 또는 async commit.

## 테스트 요구

- H2 DAO integration:
  - create event가 initial snapshot을 생성한다.
  - update event가 final state를 가진 next snapshot을 생성한다.
  - delete event가 id 기준 terminal snapshot을 생성한다.
  - 하나의 transaction 안에서 여러 change가 하나의 final snapshot을 생성한다.
  - rollback은 JaVers snapshot row를 남기지 않는다.
  - `close()`가 global hook을 unsubscribe한다.
- Module verification:
  - `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - `./gradlew :javers-exposed:test :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - `git diff --check`

## 문서

- `javers-exposed/README.md`와 `README.ko.md`에 DAO hook usage, lifecycle close,
  non-CDC boundary, delete behavior를 추가한다.
- public API KDoc은 English로 작성한다.
- 작업 후 `docs/lessons/2026-06-07-issue-138-exposed-entityhook-audit.md`를 추가한다.

## 위험과 완화

- 전역 hook 누수: subscription lifecycle을 `AutoCloseable`로 강제하고 테스트에서
  `use`/`close` 경로를 검증한다.
- 삭제 entity 재조회 실패: 삭제는 entity mapper를 호출하지 않고 id/type 기반 terminal
  snapshot으로 처리한다.
- transaction split: rollback test와 same-transaction snapshot write test로 검증한다.
- DAO object audit 위험: mapper는 detached DTO/domain object를 반환하게 문서화한다.
