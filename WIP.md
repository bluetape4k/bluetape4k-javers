# WIP - bluetape4k-javers

Snapshot: 2026-05-27 KST
Scope: 0.2.0 CQRS/Event Sourcing example split after #4 and #88 merge.
Open count: 3 issues (#5 parent, #89, #90).

## Current Evidence

- #77 merged by PR #85: README persistence option diagram.
- #3 merged by PR #86: `javers-exposed` Exposed JDBC CDO snapshot repository.
- #4 merged by PR #87: `javers-ddd` aggregate/domain-event helpers.
- #88 merged by PR #91: command-side `examples/javers-exposed-ddd` scaffold.
- #5 is the parent CQRS/Event Sourcing example issue.
- #5 has been split into #88, #89, and #90.

## Active Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| Done | [#88](https://github.com/bluetape4k/bluetape4k-javers/issues/88) command-side CQRS example scaffold | M | Merged by PR #91. |
| P1 | [#89](https://github.com/bluetape4k/bluetape4k-javers/issues/89) Kafka to Redis projection flow | L | Active. Adds query side and Testcontainers. Run serially. |
| P2 | [#90](https://github.com/bluetape4k/bluetape4k-javers/issues/90) Envers comparison benchmark results | L | Requires fresh measured data and README updates. |
| Parent | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | L | Close after #88, #89, and #90 land. |

## Dependency Map

```text
#5 examples/javers-exposed-ddd parent
  -> #88 command-side Exposed + JaVers + DDD helper flow
      -> #89 Kafka event consumer + Redis projection
          -> #90 Envers comparison benchmark results
```

## Current Direction

Work #89 next. Keep the PR limited to:

- Kafka-backed order domain event publication,
- Kafka consumer to Redis `OrderSummary` projection,
- `OrderQueryService` read API,
- Kafka and Redis Testcontainers integration coverage,
- README.md / README.ko.md command-query diagrams.

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness | 1 | none in current queue |
| JaVers implementation | 1 | done through #4 |
| Examples | 1 | #89 active; #90 waits |
