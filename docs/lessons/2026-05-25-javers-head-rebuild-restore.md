# 2026-05-25 — JaVers Redis head rebuild restore

## Context

Milestone 0.1.3 issues #62 and #76 exposed that persistent JaVers repositories
could keep snapshots and commit sequence metadata while a rebuilt repository
returned `null` from `getHeadId()`.

## Decision

Add a lazy `loadHeadId()` hook to the shared snapshot repository abstraction and
override it only for Redis-backed repositories. Lettuce restores the latest
head from the Redis sequence hash. Redisson uses a composite codec with string
map keys and long map values, then restores the latest sequence entry.

Kafka remains write-only. Its rebuild contract is tested as unsupported rather
than pretending it can restore read-side audit state.

## Outcome

Redis repository rebuilds now preserve the latest head commit id. The new tests
cover Lettuce, Redisson, and Kafka's write-only negative contract.

## Verification

- `./gradlew :javers-core:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-redis:compileTestKotlin :javers-persistence-kafka:compileTestKotlin --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:cleanTest :javers-persistence-redis:test :javers-persistence-kafka:cleanTest :javers-persistence-kafka:test --no-build-cache --no-parallel --console=plain`
- Claude advisor: `.omx/artifacts/ask-claude-code-review-javers-issue-62-convergence-20260525174339.md` (`P0=0`, `P1=0`, `APPROVE`)

## Future Guard

When a repository persists sequence metadata, add rebuild tests that instantiate
a fresh repository against existing data and assert `getHeadId()` before
claiming restart safety.
