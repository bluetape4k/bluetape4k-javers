# Issue #133 - Redis + Exposed Latency Strategy

## Context

Issue #133 asked for a Redisson near-cache latency strategy around
Exposed-backed JaVers snapshots after Redis parity guidance landed in #134.

## Decision

Do not add a new JaVers cache abstraction unless a JaVers-specific behavior gap
is proven. Keep `javers-exposed` as the durable SQL audit store, keep
`javers-persistence-redis` as a direct Redis audit store, and reuse
`bluetape4k-exposed` cache modules for read models and projections.

## Outcome

The README locale pairs now document safe cache targets, unsafe canonical audit
targets, and the strategy matrix for cache-aside, read-through, write-through,
write-behind, and near-cache.

## Verification

- Targeted module tests passed:
  `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `:javers-persistence-redis:test`: 74 tests executed.
- `:javers-exposed:test`: 53 tests executed.
- `git diff --check`: pass.

## Future Guidance

If a future issue implements composite durable history plus event/projection
behavior, it must own invalidation, replay, drain failure handling, and
repository head/commit sequence semantics explicitly. Do not route canonical
JaVers audit writes through write-behind cache behavior by default.
