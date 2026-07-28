# Issue 116 Codec Contract Review

- Date: 2026-06-07 KST
- 범위:
  - `javers-core/src/test/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepositoryCodecContractTest.kt`
  - `javers-persistence-redis/src/test/kotlin/io/bluetape4k/javers/persistence/redis/repository/RedisCdoSnapshotRepositoryCodecContractTest.kt`
  - `.github/workflows/ci.yml`

## 리뷰 요약

Repository codec contract는 production-code 변경 없이 커버된다.

- Cache2k, Caffeine, JCache는 기본값으로 `JaversCodecs.LZ4String`을 사용한다.
- Cache2k, Caffeine, JCache는 injected string codec을 통해 snapshot을 round-trip한다.
- Exposed는 plain JSON `JaversCodecs.String`을 기본값으로 사용하고, persisted `state` column을 통해 custom prefixed string codec을 round-trip한다.
- Kafka는 write-only로 유지되며 plain JSON `JaversCodecs.String` payload를 publish한다.
- Lettuce와 Redisson은 기본값으로 `JaversCodecs.LZ4Fory`를 사용하고, injected binary codec을 통해 snapshot을 round-trip한다.
- CI gitleaks installer는 release URL을 재구성하지 않고 release API에서 현재 Linux x64 asset을 resolve한다.

## PR Review 후속 조치

PR #166 review comments는 production-code 변경 없이 처리했다.

- Exposed codec contract tests는 이제 `finally` block에서 test schema를 정리한다.
- Redis codec contract tests는 shared Redis fixture에서 더 이상 `flushdb()`를 호출하지 않는다.
- Redis codec contract tests는 neighboring-test interference를 피하도록 test별 unique repository namespace를 사용한다.
- Snapshot count assertion은 이제 `shouldHaveSize`를 사용한다.

## 7-Tier Local Review

| Tier | 결과 | 증거 |
|---|---:|---|
| Spec / issue 적합성 | PASS | Issue #116 acceptance는 default repository codec coverage, 지원되는 곳의 custom codec round-trip, maintainer-visible expectation을 요구한다. |
| 정확성 | PASS | Contract tests가 default codec identity와 behavior-level snapshot round-trip 또는 Kafka publish payload decoding을 assert한다. |
| Regression risk | PASS | Production code는 변경하지 않았다. 테스트는 기존 repository constructor와 fixture를 사용한다. |
| Concurrency / infra risk | PASS | Redis와 Kafka module은 이미 JUnit parallel execution을 비활성화한다. Testcontainers-backed module은 하나의 Gradle invocation에서 검증했다. |
| Security / data safety | PASS | 테스트는 in-memory H2, local Testcontainers fixture, overridden Kafka publish future를 사용한다. secret이나 external production system은 없다. |
| 유지보수성 | PASS | Test helper는 local, small, repository-specific하며, CI install logic은 기존 gitleaks step 안에 남아 있다. |
| 검증 품질 | PASS | Targeted new-test run, clean affected-module test run, full issue acceptance command, `actionlint`, local gitleaks scan이 통과했다. |

## 결과

- P0: 0
- P1: 0
- P2: 0
- P3: 0

## Tooling Notes

- 이 worktree의 CodeGraph lookup이 zero graph nodes를 반환하여 structural graph evidence를 사용할 수 없었다.
- 이 세션에서 IntelliJ diagnostics MCP를 사용할 수 없어 Gradle compile과 targeted/full tests를 fallback evidence로 사용했다.
- 당시 사용 가능한 subagent surface가 필요한 OMX `agent_type` parameter를 노출하지 않았기 때문에 independent native review lane은 실행하지 않았다.

## 검증

```bash
./gradlew :javers-core:test --tests '*CdoSnapshotRepositoryCodecContractTest' :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryCodecContractTest' :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotRepositoryCodecContractTest' :javers-persistence-redis:test --tests '*RedisCdoSnapshotRepositoryCodecContractTest' --no-configuration-cache --no-build-cache --console=plain
```

결과: PASS, 13 tests executed.

```bash
./gradlew :javers-core:test :javers-exposed:test :javers-persistence-redis:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --console=plain
```

결과: PASS.

- `javers-core`: 181 tests
- `javers-exposed`: 27 tests
- `javers-persistence-redis`: 70 tests
- `javers-persistence-kafka`: 6 tests

```bash
./gradlew :javers-exposed:cleanTest :javers-persistence-redis:cleanTest :javers-exposed:test :javers-persistence-redis:test --no-configuration-cache --no-build-cache --console=plain
```

결과: PASS.

- `javers-exposed`: 27 tests
- `javers-persistence-redis`: 70 tests

```bash
actionlint .github/workflows/ci.yml
gitleaks detect --source . --redact --no-git --config .gitleaks.toml
```

결과: PASS.
