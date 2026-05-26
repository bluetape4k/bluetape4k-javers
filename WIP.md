# WIP - bluetape4k-javers

Snapshot: 2026-05-26 KST
Scope: 0.2.0 CQRS/Event Sourcing example split after #4 merge.
Open count: 4 issues (#5 parent, #88, #89, #90).

## Current Evidence

- #77 merged by PR #85: README persistence option diagram.
- #3 merged by PR #86: `javers-exposed` Exposed JDBC CDO snapshot repository.
- #4 merged by PR #87: `javers-ddd` aggregate/domain-event helpers.
- #5 is the parent CQRS/Event Sourcing example issue.
- #5 has been split into #88, #89, and #90.

## Active Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#88](https://github.com/bluetape4k/bluetape4k-javers/issues/88) command-side CQRS example scaffold | M | First reviewable #5 slice. H2 only; no Kafka/Redis containers. |
| P2 | [#89](https://github.com/bluetape4k/bluetape4k-javers/issues/89) Kafka to Redis projection flow | L | Adds query side and Testcontainers. Run serially. |
| P3 | [#90](https://github.com/bluetape4k/bluetape4k-javers/issues/90) Envers comparison benchmark results | L | Requires fresh measured data and README updates. |
| Parent | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | L | Close after #88, #89, and #90 land. |

## Dependency Map

```text
#5 examples/javers-exposed-ddd parent
  -> #88 command-side Exposed + JaVers + DDD helper flow
      -> #89 Kafka event consumer + Redis projection
          -> #90 Envers comparison benchmark results
```

## Current Direction

Work #88 first. Keep the first PR limited to:

- `examples/javers-exposed-ddd` module scaffold,
- `Order` aggregate and command handler,
- Exposed order table source-of-truth persistence,
- JaVers audit snapshots via `ExposedCdoSnapshotRepository`,
- in-process `DomainEventPublisher` verification,
- CI/Nightly coverage for `:javers-exposed-ddd:test`.

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness | 1 | none in current queue |
| JaVers implementation | 1 | done through #4 |
| Examples | 1 | #88 active; #89 waits |
