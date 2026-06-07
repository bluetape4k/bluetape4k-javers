# Issue #138 Exposed EntityHook audit lesson

## Context

`javers-exposed` needed a DAO lifecycle audit path that does not require every
DAO write method to call `javers.commit()` explicitly.

## Decision

Implement the feature as an explicit `EntityHook` subscription with strict
`EntityClass` mappings, detached audit-object mappers, terminal delete-by-id
snapshots, and `AutoCloseable` unsubscribe semantics.

## Outcome

The adapter remains DAO-only and keeps CDC, raw DSL writes, and publication
pipelines out of scope. Tests cover create, update, delete, rollback, final
state in one transaction, and unsubscribe behavior.

## Verification

- Exposed 1.3.0 source jar checked for `EntityHook`, `registeredChanges()`, and
  `transactionScope`.
- JaVers 7.11.0 source jar checked for `commit()` and
  `commitShallowDeleteById()`.

## Future Guard

Do not claim raw Exposed DSL or CDC coverage from `EntityHook`; add a separate
outbox or pipeline adapter when publication semantics are required.
