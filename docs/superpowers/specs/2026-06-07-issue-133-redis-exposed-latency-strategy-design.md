# Issue #133 - Redis + Exposed Latency Strategy Design

## Goal

Define the safe Redis + Exposed latency strategy for JaVers snapshots without
adding a new cache abstraction in `bluetape4k-javers`.

The design must help users decide when to use:

- `javers-exposed` as the durable JaVers audit source of truth.
- `javers-persistence-redis` as a direct Redis JaVers snapshot store.
- `bluetape4k-exposed` cache modules for query-side read models and
  projections derived from audit history.

## Evidence

- GitHub issue #133: Redisson near-cache latency strategy for Exposed-backed
  snapshots.
- Current base: `develop` at `4649eb8 test: isolate Redis parity keys`.
- Issue #134 is merged and documents Lettuce/Redisson repository selection in
  `javers-persistence-redis`.
- Existing research: `docs/research/2026-06-04-javers-multilayer-cache-pipeline.md`.
- `bluetape4k-exposed` already provides the reusable cache contract:
  - `exposed-cache`: `CacheMode`, `CacheWriteMode`, local cache config,
    resilience config, and repository test fixtures.
  - `exposed-jdbc-redisson`: read-through, write-through, write-behind, and
    near-cache variants backed by Redisson maps.
  - `exposed-jdbc-lettuce`: read-through, write-through, and write-behind
    variants backed by Lettuce.

## Scope

### Strategy Contract

- `javers-exposed` remains the canonical SQL-backed JaVers audit store for
  Exposed applications that need durable history.
- `javers-persistence-redis` remains a direct Redis-backed JaVers snapshot
  repository. It is not an Exposed-backed cache layer.
- Redis near-cache, read-through, write-through, and write-behind behavior for
  Exposed data should reuse `bluetape4k-exposed` cache modules first.
- JaVers-specific implementation is allowed only when the behavior depends on
  JaVers snapshot semantics that existing Exposed cache repositories cannot
  represent.

### Safe Cache Targets

- Application read models or projections derived from JaVers history.
- Rebuildable query results with explicit TTL, invalidation, or projection
  replay behavior.
- Exposed entity or DTO repositories that already fit the
  `bluetape4k-exposed` cache contracts.

### Unsafe Cache Targets

- Canonical `CdoSnapshot` history rows.
- JaVers commit sequence or repository head metadata.
- Global-id/version uniqueness and newest-first snapshot order.
- Audit writes where write-behind could acknowledge before durable audit state
  is committed.

These targets can be revisited only by issue #131 or a later composite
repository design that owns invalidation, replay, and failure semantics.

## Strategy Matrix

| Strategy | Use for JaVers + Exposed | Do not use for | Notes |
|---|---|---|---|
| Cache-aside | Rebuildable read models or query results | Canonical audit snapshot writes | Application code owns cache fill and invalidation. |
| Read-through | Exposed read-model repositories backed by Redisson or Lettuce | Raw `CdoSnapshot` repository replacement | Prefer existing `bluetape4k-exposed` fixtures and contracts. |
| Write-through | Mutable read models when synchronous Redis + database latency is acceptable | JaVers audit writes that must preserve commit ordering | Audit durability remains in `javers-exposed`. |
| Write-behind | Non-authoritative projections with a replay or drain policy | Audit log writes, commit sequence, repository head metadata | Failure handling must be explicit before use. |
| Near-cache | Hot read-model lookups with Redisson or Lettuce local cache support | Canonical snapshot/head state unless a composite repository owns invalidation | Use TTL and explicit invalidation for rebuildable data. |

## Public Documentation Requirements

- Update `javers-exposed/README.md` and `javers-exposed/README.ko.md` with the
  strategy matrix and explicit safe/unsafe targets.
- Update `javers-persistence-redis/README.md` and `README.ko.md` with a
  cross-reference so Redis repository users do not confuse direct Redis audit
  storage with Exposed-backed near-cache strategy.
- Keep public docs clear that this issue does not add a new production
  repository or provider-neutral JaVers cache API.

## Test and Validation Requirements

- Run existing targeted tests for both affected persistence modules:
  - `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Rely on existing `bluetape4k-exposed` cache fixture coverage for generic
  read-through, write-through, write-behind, and near-cache behavior.
- Do not duplicate generic cache behavior tests in this repository unless new
  JaVers-specific mapping code is introduced.
- Run `git diff --check`.

## Non-goals

- New production repository classes.
- New Gradle dependencies.
- Spring Boot auto-configuration.
- Composite durable history plus event stream repository; that remains issue
  #131.
- Kafka snapshot event pipeline work; that remains issues #135 and #136.

## Risks and Mitigations

- Cache misuse could hide audit ordering bugs. Mitigation: mark canonical
  snapshot and head metadata as unsafe cache targets.
- Write-behind could acknowledge audit writes before durable state exists.
  Mitigation: reject write-behind for JaVers audit writes in this issue.
- Duplicate abstractions could drift from `bluetape4k-exposed`. Mitigation:
  document reuse of existing cache contracts and fixtures.
- Documentation-only strategy could be overclaimed as implemented behavior.
  Mitigation: state that no new production repository or provider-neutral
  JaVers cache API is added.

