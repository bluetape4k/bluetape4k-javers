# WIP - bluetape4k-javers

Snapshot: 2026-05-27 KST
Scope: 0.2.0 Exposed integration hardening.
Open count: 1 issue.

## Current Evidence

- #77 merged by PR #85: README persistence option diagram.
- #3 merged by PR #86: `javers-exposed` Exposed JDBC CDO snapshot repository.
- #4 merged by PR #87: `javers-ddd` aggregate/domain-event helpers.
- #88 merged by PR #91: command-side `examples/javers-exposed-ddd` scaffold.
- #89 merged by PR #92: Kafka to Redis projection flow.
- #90 merged by PR #93: Envers comparison benchmark results.
- #5 parent is closed by the final tracking PR after #88, #89, and #90 landed.
- #95 opened to move `javers-exposed` database smoke coverage onto the shared
  `bluetape4k-exposed-jdbc-tests` H2/PostgreSQL/MySQL_V8 matrix.

## Active Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| Done | [#88](https://github.com/bluetape4k/bluetape4k-javers/issues/88) command-side CQRS example scaffold | M | Merged by PR #91. |
| Done | [#89](https://github.com/bluetape4k/bluetape4k-javers/issues/89) Kafka to Redis projection flow | L | Merged by PR #92. |
| Done | [#90](https://github.com/bluetape4k/bluetape4k-javers/issues/90) Envers comparison benchmark results | L | Merged by PR #93. |
| Done | [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5) CQRS/Event Sourcing demo | L | Parent closed after all split issues landed. |
| Active | [#95](https://github.com/bluetape4k/bluetape4k-javers/issues/95) shared Exposed JDBC DB matrix for `javers-exposed` | M | Reuses `AbstractExposedTest`, `ENABLE_DIALECTS_METHOD`, and `withTables`. |

## Dependency Map

```text
#5 examples/javers-exposed-ddd parent (complete)
  -> #88 command-side Exposed + JaVers + DDD helper flow
      -> #89 Kafka event consumer + Redis projection
          -> #90 Envers comparison benchmark results

#95 javers-exposed DB matrix
  -> bluetape4k-exposed-jdbc + bluetape4k-exposed-jdbc-tests
  -> H2 + PostgreSQL + MySQL_V8 default shared dialect set
```

## Current Direction

Finish #95 by replacing raw Testcontainers setup with the shared Exposed JDBC
test matrix and keeping unsupported dialect-specific scenarios guarded with
JUnit `Assumptions`.

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Correctness | 1 | #95 |
| JaVers implementation | 1 | done through #4 |
| Examples | 1 | done through #5 |
