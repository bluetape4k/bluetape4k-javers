# Issue #211 Kafka projection head and sequence review

## 범위

- replay-specific `projectSnapshot` repository contract를 정의한다.
- Kafka projection이 decoded snapshot을 `AbstractCdoSnapshotRepository` implementation으로 replay할 때 commit head와 sequence metadata를 restore한다.
- Exposed projection snapshot 및 sequence write를 하나의 database transaction에 유지한다.
- Lettuce Redis projection snapshot 및 sequence write를 하나의 serialized `MULTI`/`EXEC` boundary에 유지한다.
- Caffeine, Exposed H2, Lettuce Redis projection target에 대한 regression coverage를 추가한다.

## 7-Tier 검토

| Tier | 판정 | 증거 |
|---|---|---|
| 1. Correctness | PASS | `KafkaCdoSnapshotProjector`는 이제 `projectSnapshot`을 호출하며, 이는 `AbstractCdoSnapshotRepository` implementation에서 snapshot row와 commit head/sequence metadata를 restore한다. |
| 2. API and compatibility | PASS | `CdoSnapshotRepository.projectSnapshot`은 default snapshot-only implementation을 가지므로 기존 custom implementation은 source-compatible하게 유지된다. |
| 3. Head/sequence semantics | PASS | `AbstractCdoSnapshotRepository.projectSnapshot`은 duplicate commit id에 대해 existing sequence를 재사용하고, projected sequence가 더 새롭거나 같을 때만 head를 advance한다. |
| 4. Backend atomicity | PASS | Exposed는 projection write를 `inTransaction`으로 감싼다. Lettuce는 snapshot/index/sequence write를 하나의 `MULTI`/`EXEC`로 감싼다. Redisson best-effort limitation은 문서화했다. |
| 5. Kafka offset safety | PASS | Offset commit은 여전히 full polled batch가 성공적으로 project된 뒤 수행된다. decode/projection failure는 계속 `commitSync()`를 건너뛴다. |
| 6. Tests and silent failure | PASS | Caffeine, Exposed H2, Lettuce Redis tests는 `getHeadId()`와 newest-first replay ordering을 assert한다. |
| 7. Release maintainability | PASS | Projection contract는 `CdoSnapshotRepository`에 centralize되어 future projector와 repository를 하나의 replay API에 유지한다. |

## 검증

- `./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-kafka:compileKotlin :javers-persistence-kafka:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
- `./gradlew :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotProjector*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 9 tests.
- `./gradlew :javers-core:test :javers-persistence-kafka:test :javers-exposed:test :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
  - `javers-core`: 191 tests, 0 failures.
  - `javers-persistence-kafka`: 43 tests, 0 failures.
  - `javers-exposed`: 54 tests, 0 failures.
  - `javers-persistence-redis`: 76 tests, 0 failures.
- `git diff --check`
  - 결과: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
