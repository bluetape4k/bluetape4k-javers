# Issue 134 Redis parity guide

## Context

#134 needed Redis provider parity before the Redisson near-cache and composite
repository work. The existing module already had Lettuce and Redisson
repositories, provider-specific commit/shadow tests, and a codec contract.

## Decision

Add a shared Redis parity contract in test code instead of adding a
provider-neutral cache abstraction to `bluetape4k-javers`. Keep near-cache,
read-through, write-through, and write-behind behavior delegated to existing
bluetape4k cache and Exposed cache modules.

## Outcome

The shared contract now checks reverse chronological snapshots, head rebuild,
and failure propagation for both Lettuce and Redisson. It isolates test data with
unique repository prefixes ending in a short Base58 suffix instead of flushing
the whole Redis database. The module README pair explains when to choose each
provider.

## Verification

- `./gradlew :javers-persistence-redis:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` (`74 tests`)
- `git diff --check`

## Future guard

Before adding JaVers-specific Redis cache layers, first reuse or adapt
`bluetape4k-projects/cache` and `bluetape4k-exposed` cache contracts. Add
JaVers-specific tests only for snapshot ordering, commit metadata, query
behavior, and repository head semantics. Prefer unique Redis key prefixes over
`flushdb()` when a test can be isolated without clearing shared state.
