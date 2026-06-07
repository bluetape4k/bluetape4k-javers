# Issue #177 - Snapshot Event Metadata Invariants Review

## Scope

Issue #177 hardens `CdoSnapshotEventMetadata` numeric commit component
invariants in `javers-core`.

Reviewed files:

- `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/event/CdoSnapshotEvent.kt`
- `javers-core/src/test/kotlin/io/bluetape4k/javers/repository/event/CdoSnapshotEventTest.kt`
- `docs/lessons/2026-06-08-issue-177-snapshot-event-metadata-invariants.md`

## Upstream Domain Evidence

JaVers 7.11.0 source was inspected from the local Gradle source jar:

- `CommitSeqGenerator.nextId(head)` creates `CommitId(major, 0)` when no same-major id was handed out, where `major = getHeadMajorId(head) + 1`.
- `CommitSeqGenerator.getHeadMajorId(null)` returns `0`, so generated major ids start at `1`.
- Repeated synchronized sequence commits increment `minorId` from the last returned value.
- `DistributedCommitSeqGenerator.nextId()` creates a non-negative random major id with `minorId = 0`.

## Step 6-R Lite Review

| Tier | Scope | Findings | Counts |
|---|---|---|---|
| 1 Security | Event metadata construction | The change only rejects invalid metadata values earlier. It does not add parsing, deserialization, or new trust boundaries. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Replay/order metadata | Future transports can rely on commit components failing fast when invalid. No runtime retry, timeout, or lifecycle behavior changes. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Public factory boundary | The existing private-constructor plus companion factory pattern is preserved. No new public type or dependency is introduced. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Validation helpers and KDoc | Numeric guards use bluetape4k-core `requirePositiveNumber` and `requireZeroOrPositiveNumber`. KDoc now documents commit component invariants. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Regression tests | Tests now reject `commitMajorId = 0` and `commitMinorId = -1` through the public companion factory. Existing valid metadata tests still pass. | P0=0, P1=0, P2=0, P3=0 |
| 6 Performance/stability | Runtime overhead | Validation is constant-time construction work only. No hot-path allocation beyond existing factory construction. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | Lessons and issue scope | Lessons capture the upstream JaVers commit-id rule. README, CI, Nightly, BOM, and changelog updates are not needed. The optional `author` blank question remains out of this numeric invariant fix because current JaVers commit metadata owns that value and the issue asks to consider, not require, a new author contract. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R lite verdict: PASS with P0=0 and P1=0.

## Evidence

- `CdoSnapshotEvent.kt`: `commitMajorId.requirePositiveNumber("commitMajorId")`.
- `CdoSnapshotEvent.kt`: `commitMinorId.requireZeroOrPositiveNumber("commitMinorId")`.
- `CdoSnapshotEventTest.kt`: public factory rejects zero major and negative minor inputs.

## Validation Evidence

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: PASS, 184 tests executed.
- `git diff --check`
  - Result: PASS, no whitespace errors.

## Final Gate

P0=0. P1=0. PR creation is allowed.
