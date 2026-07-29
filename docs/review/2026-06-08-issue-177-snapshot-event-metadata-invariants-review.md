# Issue #177 - Snapshot Event Metadata Invariants 검토

## 범위

Issue #177은 `javers-core`의 `CdoSnapshotEventMetadata` numeric commit component
invariant를 강화한다.

검토한 파일:

- `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/event/CdoSnapshotEvent.kt`
- `javers-core/src/test/kotlin/io/bluetape4k/javers/repository/event/CdoSnapshotEventTest.kt`
- `docs/lessons/2026-06-08-issue-177-snapshot-event-metadata-invariants.md`

## Upstream domain 증거

local Gradle source jar에서 JaVers 7.11.0 source를 inspected했다.

- `CommitSeqGenerator.nextId(head)`는 same-major id가 아직 발급되지 않았을 때 `CommitId(major, 0)`을 만든다. 여기서 `major = getHeadMajorId(head) + 1`이다.
- `CommitSeqGenerator.getHeadMajorId(null)`은 `0`을 반환하므로 generated major id는 `1`에서 시작한다.
- Repeated synchronized sequence commit은 마지막 반환값에서 `minorId`를 증가시킨다.
- `DistributedCommitSeqGenerator.nextId()`는 `minorId = 0`인 non-negative random major id를 만든다.

## Step 6-R Lite 검토

| Tier | 범위 | 결과 | Counts |
|---|---|---|---|
| 1 Security | Event metadata construction | 변경은 invalid metadata value를 더 일찍 reject할 뿐이다. parsing, deserialization, new trust boundary를 추가하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Ops/SRE | Replay/order metadata | future transport는 invalid commit component가 fail fast한다고 신뢰할 수 있다. runtime retry, timeout, lifecycle behavior 변경은 없다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Structural | Public factory boundary | 기존 private-constructor plus companion factory pattern을 보존한다. 새 public type 또는 dependency는 도입하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API 품질 | 검증 helper와 KDoc | Numeric guard는 bluetape4k-core `requirePositiveNumber`와 `requireZeroOrPositiveNumber`를 사용한다. KDoc은 이제 commit component invariant를 문서화한다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/types/silent failure | Regression tests | 테스트는 public companion factory를 통해 `commitMajorId = 0`과 `commitMinorId = -1`을 reject한다. 기존 valid metadata tests도 계속 통과한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 성능/안정성 | Runtime overhead | 검증은 constant-time construction work뿐이다. 기존 factory construction 외의 hot-path allocation은 없다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Documentation/release/evidence | Lesson 및 issue scope | Lesson은 upstream JaVers commit-id rule을 기록한다. README, CI, Nightly, BOM, changelog update는 필요하지 않다. optional `author` blank question은 이 numeric invariant fix의 scope 밖에 남는다. 현재 JaVers commit metadata가 해당 값을 소유하고, issue는 새 author contract를 요구하는 것이 아니라 검토를 요구하기 때문이다. | P0=0, P1=0, P2=0, P3=0 |

Step 6-R lite 판정: P0=0, P1=0으로 PASS.

## 증거

- `CdoSnapshotEvent.kt`: `commitMajorId.requirePositiveNumber("commitMajorId")`.
- `CdoSnapshotEvent.kt`: `commitMinorId.requireZeroOrPositiveNumber("commitMinorId")`.
- `CdoSnapshotEventTest.kt`: public factory rejects zero major and negative minor inputs.

## 검증 증거

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - 결과: PASS, 184 tests executed.
- `git diff --check`
  - 결과: PASS, no whitespace errors.

## 최종 gate 판정

P0=0. P1=0. PR 생성을 허용한다.
