---
title: JaVers Exposed EntityHook flush audit research
date: 2026-06-04
source_type: official-docs-plus-source-jar-check
repo: bluetape4k/bluetape4k-javers
related_issue: 138
---

# JaVers Exposed EntityHook flush audit 연구

## Source link

- JetBrains Exposed 1.3.0 DAO documentation:
  https://github.com/jetbrains/exposed/blob/1.3.0/documentation-website/Writerside/topics/Get-started-with-Exposed-DAO.md
- Local Exposed 1.3.0 source jar:
  `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.exposed/exposed-dao/1.3.0/.../exposed-dao-1.3.0-sources.jar`
- JaVers 7.11.0 `Javers` API source:
  `~/.gradle/caches/modules-2/files-2.1/org.javers/javers-core/7.11.0/.../javers-core-7.11.0-sources.jar`
- 현재 `javers-ddd` aggregate repository:
  `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt`
- 현재 Exposed-backed JaVers repository:
  `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt`

## Repo-local 사실

- `bluetape4k-javers`는 현재 Exposed `1.3.0`과 JaVers `7.11.0`을 사용한다.
- `javers-exposed`는 `ExposedCdoSnapshotRepository`를 통해 JaVers commit metadata와 full encoded `CdoSnapshot` payload를 persist한다.
- `ExposedCdoSnapshotRepository`는 각 repository operation을 `transaction(database) {}` 또는 `transaction {}`로 감싼다. Flush-based adapter는 JaVers write가 source entity mutation과 같은 effective transaction 안에 남는지 test해야 한다.
- `javers-ddd`는 현재 explicit audit commit을 수행한다. Subclass persistence가 먼저 실행되고, 이어서 `AggregateRepository.save()`가 `javers.commit(author, saved, properties)`를 호출한다.
- `examples/javers-exposed-ddd`는 explicit pattern을 따른다. Exposed table write가 source-of-truth persistence이고 JaVers audit은 `AggregateRepository`를 통해 delegate된다.

## Exposed flush 및 hook 동작

Exposed DAO documentation은 DAO property change가 memory에 cache되고 다음 read operation 전 또는 transaction 끝에서 flush된다고 설명한다. 1.3.0 source는 관련 hook surface를 다음과 같이 확인해 준다.

- `EntityHook.subscribe(action)`은 `EntityChange` event용 global subscriber를 등록한다.
- `EntityChangeType`에는 `Created`, `Updated`, `Removed`가 있다.
- `Transaction.registerChange()`는 transaction-local entity change를 기록하고 adjacent identical event만 de-duplicate한다.
- `Transaction.registeredChanges()`는 transaction에 등록된 모든 entity change를 반환한다.
- `EntityCache.flush()`는 pending insert와 update를 persist한다.
- `EntityLifecycleInterceptor.beforeCommit()`은 `transaction.flushCache()`를 호출한 뒤 `transaction.alertSubscribers()`를 호출하고, hook이 생성한 entity를 flush한다.
- `Entity.delete()`는 delete statement 실행 전에 `Removed` event를 등록한다.
- `Entity.flush()`는 write value가 persisted될 때 `Updated` event를 등록한다.

이 구조 덕분에 DAO-only JaVers adapter가 가능하다. Entity lifecycle change를 subscribe하고 flushed DAO entity를 audit object로 map한 뒤 Exposed transaction이 아직 active인 동안 JaVers를 호출하면 된다.

## JaVers API implication

JaVers에는 관련 commit path가 두 개 있다.

- `commit(author, currentVersion, properties)` for created and updated objects.
- `commitShallowDelete()` / `commitShallowDeleteById()` for terminal delete snapshots.

Delete handling이 주요 special case다. `Removed` change는 Exposed가 delete를 실행하기 전에 등록되지만, subscriber가 event를 받을 때는 row가 이미 제거됐거나 entity cache가 invalidated되어 `EntityChange.toEntity()`가 `null`을 반환할 수 있다. Safe implementation은 다음 중 하나를 선택해야 한다.

- delete 전에 충분한 delete identity data를 capture하고 `commitShallowDeleteById()`를 사용한다.
- Full terminal object state가 필요한 audited entity를 위해 explicit delete-before-snapshot path를 제공한다.

Created 및 updated change는 database row와 generated id가 이미 available하므로 보통 flush 후 `EntityChange.toEntity()`에서 map할 수 있다.

## 제안 설계 방향

Generic CDC layer가 아니라 Exposed DAO audit adapter를 만든다.

- Scope는 Exposed DAO `Entity` lifecycle event로 제한한다.
- Raw Exposed DSL `Table.insert`, `update`, `deleteWhere`, `upsert`, external database write support를 주장하지 않는다.
- `ExposedJaversEntityHookSubscription` 같은 explicit lifecycle object를 통해 global `EntityHook` subscriber를 등록한다.
- Unrelated DAO change를 무시하도록 configured `EntityClass` / entity type 기준으로 event를 filter한다.
- Exposed transaction id와 entity key별로 change를 buffer 및 coalesce한다.
- Transaction 안의 모든 intermediate property assignment가 아니라 entity의 final state만 commit한다.
- JaVers repository write가 같은 audit subscriber를 recursively trigger하지 않도록 transaction-scoped reentrancy guard를 사용한다.
- DAO entity에서 JaVers domain object 또는 DTO로 가는 mapper를 노출한다. Detached domain model이 더 안전한 경우 transaction-bound DAO object를 직접 audit하도록 강제하지 않는다.
- Author와 commit property는 `UserContext`, explicit callback, transaction-local metadata provider에서 올 수 있게 한다.

최소 implementation 형태는 다음과 같다.

1. `EntityHook`을 subscribe한다.
2. `Created` / `Updated`에서는 entity를 resolve하고 detached audit object로 map한 뒤 transaction id와 entity key별로 queue한다.
3. `Removed`에서는 captured identity data에서 terminal snapshot instruction을 queue한다.
4. Transaction flush/commit notification에서 reentrancy guard 아래 JaVers commit을 실행한다.
5. `ExposedCdoSnapshotRepository`로 rollback behavior를 검증한다.

## Transaction 및 virtual-thread 제약

선호 runtime은 Java virtual thread 아래의 JDBC-backed Exposed다. Code가 blocking JDBC이므로 current JaVers Exposed repository 방향과 호환되며, virtual thread는 request/thread contention을 줄인다.

구현 제약:

- `synchronized`와 `@Synchronized`를 피한다.
- Global mutable state보다 transaction-local state를 선호한다.
- Explicit lock, concurrent map, bounded queue는 필요하고 virtual-thread friendly할 때만 사용한다.
- Entity hook 안에서 Redis/Kafka/NATS/SQS publication을 직접 수행하지 않는다. Hook work는 transaction 안에 audit state 또는 outbox record를 persist해야 하며 asynchronous publication은 pipeline adapter work에 속한다.
- 가능하면 virtual-thread executor로 audit path를 실행하는 test를 추가한다.

## Risk

- `EntityHook` subscriber는 global이다. Long-lived adapter는 explicit unsubscribe/close semantics와 strict event filtering을 지원해야 한다.
- `javers.commit()`이 Exposed statement를 통해 snapshot을 persist하므로 reentrancy는 실제 risk다.
- `ExposedCdoSnapshotRepository`는 현재 operation마다 `transaction(database) {}`를 연다. Adapter는 이것이 source write와 audit write를 accidental하게 independent transaction으로 split하지 않음을 증명해야 한다.
- Entity delete에는 별도 terminal snapshot strategy가 필요하다.
- DAO object를 직접 audit하면 lazy reference 또는 transaction-bound state가 JaVers로 끌려 들어갈 수 있다. Detached domain object 또는 DTO를 default recommendation으로 삼아야 한다.

## 권장 issue

Issue #138은 DAO-only EntityHook flush audit adapter 구현을 추적한다.

Acceptance criteria에는 created, updated, removed, one transaction의 repeated change, rollback, reentrancy guard, `ExposedCdoSnapshotRepository`와의 same-transaction persistence, virtual-thread friendliness test가 포함되어야 한다.

## Assets

Image asset은 필요하지 않았다.
