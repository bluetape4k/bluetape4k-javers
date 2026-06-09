# Issue 104 Spring Boot 4 Auto-Configuration Plan

## Scope

Implement a Spring Boot 4 auto-configuration module for existing JaVers Exposed,
Redis, and Kafka repository backends.

## Steps

1. Create `javers-spring-boot4-autoconfigure` and register it in Gradle.
2. Add configuration properties and backend-specific auto-configuration phases.
3. Add `ApplicationContextRunner` tests for backend selection, backoff, and
   optional-class absence.
4. Add module README files and update root README locale set, CHANGELOG, and
   repo `AGENTS.md`.
5. Register the module in CI and Nightly test/coverage workflows.
6. Validate with Gradle compile/test/Kover, `actionlint`, `git diff --check`,
   and 7-Tier review.
7. Create PR against `develop`; merge only after explicit user approval.

## Validation Commands

```bash
./gradlew projects --no-configuration-cache --no-build-cache --console=plain
./gradlew :javers-spring-boot4-autoconfigure:compileKotlin :javers-spring-boot4-autoconfigure:compileTestKotlin :javers-spring-boot4-autoconfigure:test :javers-spring-boot4-autoconfigure:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain
./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-kafka:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain
./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain
actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml
git diff --check
```

## Plan Review

| Tier | Result | Notes |
|---|---|---|
| Security | PASS | No credential creation or secret handling planned. |
| Ops/SRE | PASS | Conditional startup and caller-owned clients reduce surprise side effects. |
| Structural | PASS | Separate module avoids widening existing repository modules. |
| Kotlin | PASS | Uses Spring Boot 4 slice auto-configuration conventions. |
| Tests | PASS | Tests target all supported backend branches and backoff behavior. |
| Performance | PASS | No background polling, retries, or thread ownership added. |
| Docs/Release | PASS | Module registration and README locale updates are explicit plan items. |

Final gate: `P0 = 0`, `P1 = 0`.
