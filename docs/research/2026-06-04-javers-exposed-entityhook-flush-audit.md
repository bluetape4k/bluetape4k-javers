---
title: JaVers Exposed EntityHook flush audit research
date: 2026-06-04
source_type: official-docs-plus-source-jar-check
repo: bluetape4k/bluetape4k-javers
related_issue: 138
---

# JaVers Exposed EntityHook flush audit research

## Source links

- JetBrains Exposed 1.3.0 DAO documentation:
  https://github.com/jetbrains/exposed/blob/1.3.0/documentation-website/Writerside/topics/Get-started-with-Exposed-DAO.md
- Local Exposed 1.3.0 source jar:
  `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.exposed/exposed-dao/1.3.0/.../exposed-dao-1.3.0-sources.jar`
- JaVers 7.11.0 `Javers` API source:
  `~/.gradle/caches/modules-2/files-2.1/org.javers/javers-core/7.11.0/.../javers-core-7.11.0-sources.jar`
- Current `javers-ddd` aggregate repository:
  `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd/AggregateRepository.kt`
- Current Exposed-backed JaVers repository:
  `javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt`

## Repo-local facts

- `bluetape4k-javers` currently uses Exposed `1.3.0` and JaVers `7.11.0`.
- `javers-exposed` persists JaVers commit metadata and full encoded `CdoSnapshot` payloads through `ExposedCdoSnapshotRepository`.
- `ExposedCdoSnapshotRepository` wraps each repository operation in `transaction(database) {}` or `transaction {}`. A flush-based adapter must test that JaVers writes stay in the same effective transaction as the source entity mutation.
- `javers-ddd` currently performs explicit audit commits: subclass persistence runs first, then `AggregateRepository.save()` calls `javers.commit(author, saved, properties)`.
- `examples/javers-exposed-ddd` follows the explicit pattern: Exposed table writes are source-of-truth persistence and JaVers audit is delegated through `AggregateRepository`.

## Exposed flush and hook behavior

The Exposed DAO documentation states that DAO property changes are cached in memory and flushed before the next read operation or at the end of the transaction. The 1.3.0 source confirms the relevant hook surface:

- `EntityHook.subscribe(action)` registers a global subscriber for `EntityChange` events.
- `EntityChangeType` has `Created`, `Updated`, and `Removed`.
- `Transaction.registerChange()` records transaction-local entity changes and de-duplicates only adjacent identical events.
- `Transaction.registeredChanges()` returns all entity changes registered in the transaction.
- `EntityCache.flush()` persists pending inserts and updates.
- `EntityLifecycleInterceptor.beforeCommit()` calls `transaction.flushCache()`, then `transaction.alertSubscribers()`, then flushes any entities created by hooks.
- `Entity.delete()` registers a `Removed` event before executing the delete statement.
- `Entity.flush()` registers an `Updated` event when write values are persisted.

This makes a DAO-only JaVers adapter feasible: subscribe to entity lifecycle changes, map the flushed DAO entity to an audit object, and call JaVers while the Exposed transaction is still active.

## JaVers API implications

JaVers has two relevant commit paths:

- `commit(author, currentVersion, properties)` for created and updated objects.
- `commitShallowDelete()` / `commitShallowDeleteById()` for terminal delete snapshots.

Delete handling is the main special case. A `Removed` change is registered before Exposed executes the delete, but by the time a subscriber receives the event, `EntityChange.toEntity()` may return `null` because the row has already been removed or the entity cache has been invalidated. A safe implementation should either:

- capture enough delete identity data before delete and use `commitShallowDeleteById()`, or
- provide an explicit delete-before-snapshot path for audited entities that need full terminal object state.

Created and updated changes can usually map from `EntityChange.toEntity()` after flush because the database row and generated id are already available.

## Proposed design direction

Create an Exposed DAO audit adapter, not a generic CDC layer:

- Scope it to Exposed DAO `Entity` lifecycle events.
- Do not claim support for raw Exposed DSL `Table.insert`, `update`, `deleteWhere`, `upsert`, or external database writes.
- Register a global `EntityHook` subscriber through an explicit lifecycle object such as `ExposedJaversEntityHookSubscription`.
- Filter events by configured `EntityClass` / entity type so unrelated DAO changes are ignored.
- Buffer and coalesce changes per Exposed transaction id and entity key.
- Commit only the final state of an entity within a transaction, not every intermediate property assignment.
- Use a transaction-scoped reentrancy guard so JaVers repository writes do not recursively trigger the same audit subscriber.
- Expose a mapper from DAO entity to JaVers domain object or DTO. Do not force users to audit transaction-bound DAO objects directly when a detached domain model is safer.
- Allow author and commit properties to come from a `UserContext`, explicit callback, or transaction-local metadata provider.

The minimal implementation shape is:

1. Subscribe to `EntityHook`.
2. On `Created` / `Updated`, resolve the entity, map it to a detached audit object, and queue it by transaction id and entity key.
3. On `Removed`, queue a terminal snapshot instruction from captured identity data.
4. On transaction flush/commit notification, run JaVers commits under a reentrancy guard.
5. Verify rollback behavior with `ExposedCdoSnapshotRepository`.

## Transaction and virtual-thread constraints

The preferred runtime is JDBC-backed Exposed under Java virtual threads. This is compatible with the current JaVers Exposed repository direction because the code is blocking JDBC, but virtual threads reduce request/thread contention.

Implementation constraints:

- Avoid `synchronized` and `@Synchronized`.
- Prefer transaction-local state over global mutable state.
- Use explicit locks, concurrent maps, or bounded queues only when they are necessary and virtual-thread friendly.
- Do not perform Redis/Kafka/NATS/SQS publication directly inside the entity hook. Hook work should persist audit state or an outbox record in the transaction; asynchronous publication belongs to the pipeline adapter work.
- Add tests that run the audit path with a virtual-thread executor when possible.

## Risks

- `EntityHook` subscribers are global. A long-lived adapter must support explicit unsubscribe/close semantics and strict event filtering.
- Reentrancy is real because `javers.commit()` persists snapshots through Exposed statements.
- `ExposedCdoSnapshotRepository` currently opens `transaction(database) {}` for each operation. The adapter must prove this does not accidentally split source writes and audit writes into independent transactions.
- Deleting entities needs a separate terminal snapshot strategy.
- Auditing DAO objects directly can pull lazy references or transaction-bound state into JaVers. Detached domain objects or DTOs should be the default recommendation.

## Recommended issue

Issue #138 tracks implementation of the DAO-only EntityHook flush audit adapter.

Acceptance criteria should include tests for created, updated, removed, repeated changes in one transaction, rollback, reentrancy guard, same-transaction persistence with `ExposedCdoSnapshotRepository`, and virtual-thread friendliness.

## Assets

No image assets were needed.
