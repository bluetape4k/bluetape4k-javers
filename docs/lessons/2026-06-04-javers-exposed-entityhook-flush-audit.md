# JaVers Exposed EntityHook flush audit lesson

## Context

The JaVers Exposed integration may be able to audit Exposed DAO entities from
the Exposed flush lifecycle instead of requiring every repository to call
`javers.commit()` explicitly.

## Decision

Preserve the research as a repo-local note and track implementation in issue
#138. Treat the feature as a DAO-only `EntityHook` adapter, not as generic CDC.

## Outcome

The research identifies a feasible path using Exposed 1.3.0 `EntityHook`,
`EntityChange`, transaction-local registered changes, and JaVers
`commit()` / `commitShallowDeleteById()` APIs. The design must include
reentrancy protection, delete semantics, transaction-bound tests, and
virtual-thread-friendly JDBC behavior.

## Verification

- Exposed 1.3.0 source jar inspected for `EntityHook`, `EntityCache`,
  `EntityLifecycleInterceptor`, and `Entity`.
- JaVers 7.11.0 source jar inspected for commit and shallow delete APIs.
- Current `AggregateRepository` and `ExposedCdoSnapshotRepository` inspected.

## Future Work

Implement #138 without duplicating `javers-exposed` snapshot persistence. Keep
message publication out of the hook and route it through outbox or the pipeline
adapter issues.
