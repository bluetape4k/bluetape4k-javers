# Issue #209 commit-atomic snapshot persistence review

## 범위

- 모든 snapshot과 commit sequence metadata를 하나의 commit unit으로 쓰는 repository hook을 도입한다.
- Exposed snapshot persistence를 단일 database transaction으로 감싼다.
- Lettuce Redis snapshot write와 sequence metadata를 하나의 `MULTI`/`EXEC` boundary로 batch 처리한다.
- 현재 data structure가 같은 commit-level atomicity를 제공하지 않으므로 Redisson의 best-effort boundary를 문서화한다.
- multi-snapshot Exposed commit에 대한 failure-injection coverage를 추가한다.

## 7-Tier 검토

| Tier | 판정 | 증거 |
|---|---|---|
| 1. Correctness | PASS | `persist`는 이제 sequence 하나를 reserve하고 snapshot 및 sequence metadata를 `persistCommit`에 delegate한다. `head`는 해당 call이 return된 뒤에만 advance된다. |
| 2. API and compatibility | PASS | 새 `persistCommit` hook은 `protected open`이며 public repository API를 보존한다. |
| 3. Atomicity semantics | PASS | Exposed는 전체 commit을 `inTransaction`으로 감싼다. Lettuce는 `exec()` 전에 모든 snapshot write와 sequence update를 queue한다. |
| 4. Failure behavior | PASS | Exposed regression test는 첫 snapshot encode 후 실패하고 zero snapshot rows, zero commit rows, no head id를 검증한다. |
| 5. Redis consistency | PASS | Lettuce는 `MULTI` 전에 snapshot을 encode하고 기존 transaction lock으로 `MULTI`/`EXEC`를 serialize한다. 기존 Redis parity tests는 green으로 유지된다. |
| 6. Documentation/KDoc | PASS | Core와 Lettuce KDoc은 commit-level persistence를 설명한다. Redisson KDoc은 best-effort limitation을 명시적으로 문서화한다. |
| 7. Release maintainability | PASS | hook은 JaVers-facing repository usage를 바꾸지 않고 future backend-specific atomicity를 centralize한다. |

## 검증

- `./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS.
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 191 tests.
- `./gradlew :javers-exposed:test --tests '*ExposedCdoSnapshotRepositoryH2Test*' --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 9 tests.
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 76 tests.
- `git diff --check`
  - 결과: PASS.

## P0/P1 Status

P0: 0.
P1: 0.
