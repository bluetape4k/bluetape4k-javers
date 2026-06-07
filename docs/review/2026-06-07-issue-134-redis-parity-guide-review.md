# Issue 134 Redis parity guide review

## Scope

- `javers-persistence-redis` Redis repository contract tests.
- `javers-persistence-redis` English/Korean README selection guidance.
- `docs/lessons/2026-06-07-issue-134-redis-parity-guide.md`.

## 7-Tier lite findings

- Tier 1 correctness: PASS. Lettuce and Redisson now inherit the same JaVers
  Redis parity tests for reverse chronological snapshots, head rebuild, and
  failed encode propagation.
- Tier 1 isolation update: PASS. PR review feedback on `flushdb()` risk was
  addressed by using `provider + contract + Base58` repository names in the
  parity/commit contract.
- Tier 2 API/contract: PASS. No production API or provider-neutral cache
  abstraction was added.
- Tier 3 integration: PASS. Existing provider-specific shadow tests and codec
  contract remain in place.
- Tier 4 implementation quality: PASS. Provider-specific commit tests now only
  provide client creation, Redis flush, and repository factory wiring.
- Tier 5 test quality: PASS. Redis Testcontainers module test ran serially and
  executed 74 tests successfully.
- Tier 6 operations: PASS. No workflow, CI, module registration, or dependency
  changes.
- Tier 7 documentation/evidence: PASS. README and README.ko.md were updated
  together and the lesson records the reuse guard.

## Findings

- P0: 0
- P1: 0
- P2: 0

## Evidence

- CodeGraph detect-changes fallback returned no mapped Kotlin nodes for this
  worktree; manual diff review was used as the authoritative review gate.
- `./gradlew :javers-persistence-redis:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` (`74 tests`)
- `git diff --check`

## Residual risk

No production Redis write path changed. The remaining #133/#131 work still needs
fresh tests when near-cache or composite repository behavior is introduced.
Existing shadow tests still flush Redis as pre-existing module behavior; this PR
removes `flushdb()` from the newly shared parity/commit contract only.
