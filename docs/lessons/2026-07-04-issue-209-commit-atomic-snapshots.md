# Issue #209 commit-atomic snapshot persistence

## Context

`AbstractCdoSnapshotRepository.persist` wrote each snapshot first and updated
commit sequence metadata after the loop. Backends with transactional primitives
could not wrap a whole JaVers commit without duplicating the base persistence
algorithm, so a mid-commit failure could leave partial snapshot data.

## Decision

Add a protected `persistCommit` hook that receives the already reserved sequence
for the JaVers commit. Exposed overrides it with a single database transaction,
and Lettuce Redis overrides it with one serialized `MULTI`/`EXEC` boundary for
all snapshot writes and the sequence update. Redisson remains explicitly
documented as best-effort because the current Redisson structures do not provide
an equivalent commit-level transaction.

## Outcome

Exposed now rolls back multi-snapshot commit failures without advancing head or
leaving commit metadata. Lettuce keeps snapshot rows and sequence metadata in
one Redis transaction. The public repository API remains unchanged.

## Future Guidance

When adding a durable snapshot repository, override `persistCommit` if the
backend can provide commit-level atomicity. If the backend cannot, document the
best-effort failure mode next to the repository contract instead of implying
stronger consistency than the storage primitive can provide.
