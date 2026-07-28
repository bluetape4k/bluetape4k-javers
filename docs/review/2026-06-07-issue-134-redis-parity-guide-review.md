# Issue 134 Redis parity guide review

## 범위

- `javers-persistence-redis` Redis repository contract tests.
- `javers-persistence-redis` English/Korean README selection guidance.
- `docs/lessons/2026-06-07-issue-134-redis-parity-guide.md`.

## 7-Tier lite findings

- Tier 1 correctness: PASS. Lettuce와 Redisson은 reverse chronological snapshots,
  head rebuild, failed encode propagation에 대해 같은 JaVers Redis parity tests를
  상속한다.
- Tier 1 isolation update: PASS. `flushdb()` risk에 대한 PR review feedback은
  parity/commit contract에서 `provider + contract + Base58` repository name을
  사용하도록 처리했다.
- Tier 2 API/contract: PASS. Production API 또는 provider-neutral cache
  abstraction은 추가하지 않았다.
- Tier 3 integration: PASS. 기존 provider-specific shadow tests와 codec
  contract는 유지된다.
- Tier 4 implementation quality: PASS. Provider-specific commit tests는 이제
  client creation, Redis flush, repository factory wiring만 제공한다.
- Tier 5 test quality: PASS. Redis Testcontainers module test를 직렬로 실행했고
  74 tests가 성공했다.
- Tier 6 operations: PASS. workflow, CI, module registration, dependency
  변경은 없다.
- Tier 7 documentation/evidence: PASS. README와 README.ko.md를 함께 갱신했고
  lesson은 reuse guard를 기록한다.

## 결과

- P0: 0
- P1: 0
- P2: 0

## 증거

- 이 worktree에서 CodeGraph detect-changes fallback이 mapped Kotlin node를 반환하지
  않아 manual diff review를 authoritative review gate로 사용했다.
- `./gradlew :javers-persistence-redis:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` (`74 tests`)
- `git diff --check`

## 잔여 위험

Production Redis write path는 변경하지 않았다. 남은 #133/#131 작업에서 near-cache 또는
composite repository behavior가 도입되면 fresh tests가 여전히 필요하다. 기존 shadow
tests는 pre-existing module behavior로 Redis를 계속 flush한다. 이 PR은 새로 공유된
parity/commit contract에서만 `flushdb()`를 제거한다.
