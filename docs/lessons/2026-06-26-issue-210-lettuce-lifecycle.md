# Lessons Learned - Issue 210 Lettuce Repository Lifecycle (2026-06-26)

Related issue: #210
Affected modules: `javers-persistence-redis`, `javers-spring-boot4-autoconfigure`

## Context

`LettuceCdoSnapshotRepository` accepted a caller-provided `RedisClient`, but
created lazy read/write Lettuce command handles without an explicit repository
shutdown contract. Spring auto-configuration could therefore create the
repository as a bean while leaving repository-created connection resources
ambiguous at context shutdown.

## Decision

Keep `RedisClient` caller-owned and make the repository own only the read/write
connections it opens from that client. The repository implements
`AutoCloseable`, closes initialized read/write connections idempotently, and
does not call `RedisClient.shutdown()`.

## Outcome

The persistence test now proves read/write connections opened by a commit are
closed by `LettuceCdoSnapshotRepository.close()`. The Spring Boot
auto-configuration test proves context shutdown invokes repository cleanup while
leaving the application-owned `RedisClient` alive.

## Verification

- `./gradlew :javers-persistence-redis:test --tests '*LettuceJaversCommitTest*' :javers-spring-boot4-autoconfigure:test --tests '*JaversAutoConfigurationTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain` - PASS, 7 + 18 tests.
- `./gradlew :javers-persistence-redis:test :javers-spring-boot4-autoconfigure:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` - PASS, 76 + 18 tests.

## Future Guard

When a repository accepts a caller-owned client but opens its own connection or
producer handles, document the ownership split and add a shutdown test that
proves both resource cleanup and caller ownership preservation.
