# Issue #208 aggregate save boundary

## Context

`AggregateRepository.save` persisted source state, committed JaVers audit state,
and then published events as separate sequential effects. Exposed-backed example
repositories opened their own source transaction inside `persist`, so JaVers
failure could leave source rows without audit history.

## Decision

Add a protected `saveAuditBoundary` hook to the base repository and override it
in Exposed-backed examples with `transaction(database)`. Keep event publication
after the source/audit boundary and treat publisher failure as a propagated
best-effort failure that does not roll back committed source/audit state.

## Outcome

Failure-injection tests now prove rollback on JaVers commit failure and retained
source/audit state on publisher failure.

## Future Guidance

When adding a durable `AggregateRepository` adapter, override
`saveAuditBoundary` if the source store and JaVers repository can share a
transaction. If publisher rollback is required, add an explicit outbox instead
of moving synchronous publisher calls back inside the audit transaction.
