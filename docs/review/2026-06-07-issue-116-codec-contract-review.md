# Issue 116 Codec Contract Review

- Date: 2026-06-07 KST
- Scope:
  - `javers-core/src/test/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedisCdoSnapshotRepositoryCodecContractTest.kt`
  - `.github/workflows/ci.yml`

## Review Summary

Repository codec contracts are covered without production-code changes:

- Cache2k, Caffeine, and JCache default to `JaversCodecs.LZ4String`.
- Cache2k, Caffeine, and JCache round-trip snapshots through an injected string codec.
- Exposed defaults to plain JSON `JaversCodecs.String` and round-trips a custom prefixed string codec through the persisted `state` column.
- Kafka remains write-only and publishes plain JSON `JaversCodecs.String` payloads.
- Lettuce and Redisson default to `JaversCodecs.LZ4Fory` and round-trip snapshots through an injected binary codec.
- The CI gitleaks installer resolves the current Linux x64 asset from the release API instead of reconstructing a release URL.

## 7-Tier Local Review

| Tier | Result | Evidence |
|---|---:|---|
| Spec / issue fit | PASS | Issue #116 acceptance requires default repository codec coverage, custom codec round-trip where supported, and maintainer-visible expectations. |
| Correctness | PASS | Contract tests assert default codec identity and behavior-level snapshot round-trip or Kafka publish payload decoding. |
| Regression risk | PASS | No production code changed; tests use existing repository constructors and fixtures. |
| Concurrency / infra risk | PASS | Redis and Kafka modules already disable JUnit parallel execution; Testcontainers-backed modules were verified in one Gradle invocation. |
| Security / data safety | PASS | Tests use in-memory H2, local Testcontainers fixtures, and overridden Kafka publish futures; no secrets or external production systems. |
| Maintainability | PASS | Test helpers are local, small, and repository-specific; CI install logic remains within the existing gitleaks step. |
| Validation quality | PASS | Targeted new-test run, full issue acceptance command, `actionlint`, and local gitleaks scan passed. |

## Findings

- P0: 0
- P1: 0
- P2: 0
- P3: 0

## Tooling Notes

- CodeGraph lookup for this worktree returned zero graph nodes, so structural graph evidence was unavailable.
- IntelliJ diagnostics MCP was unavailable in this session; Gradle compile and targeted/full tests were used as fallback evidence.
- Independent native review lanes were not launched because the available subagent surface did not expose the required OMX `agent_type` parameter.

## Verification

```bash
./gradlew :javers-core:test --tests '*CdoSnapshotRepositoryCodecContractTest' :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryCodecContractTest' :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotRepositoryCodecContractTest' :javers-persistence-redis:test --tests '*RedisCdoSnapshotRepositoryCodecContractTest' --no-configuration-cache --no-build-cache --console=plain
```

Result: PASS, 13 tests executed.

```bash
./gradlew :javers-core:test :javers-exposed:test :javers-persistence-redis:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --console=plain
```

Result: PASS.

- `javers-core`: 181 tests
- `javers-exposed`: 27 tests
- `javers-persistence-redis`: 70 tests
- `javers-persistence-kafka`: 6 tests

```bash
actionlint .github/workflows/ci.yml
gitleaks detect --source . --redact --no-git --config .gitleaks.toml
```

Result: PASS.
