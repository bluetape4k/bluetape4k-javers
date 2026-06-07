# Lessons Learned - Issue 177 Snapshot Event Metadata Invariants

**관련 이슈**: #177
**영향 모듈**: `javers-core`

## L1: transport metadata factory는 JaVers 생성 규칙을 다시 고정한다

### 문제

`CdoSnapshotEventMetadata.from(snapshot, ...)`는 JaVers가 만든 `CommitId`에서
valid `majorId`와 `minorId`를 복사하지만, public companion factory는 외부
adapter나 tests가 직접 호출할 수 있다. 기존 factory는 required string,
`snapshotVersion`, `repositorySequence`만 검증했고 commit component 숫자
범위는 그대로 통과시켰다.

### 결정

JaVers 7.11.0 source 기준으로 generated commit id는 `majorId`가 1부터
시작하고, `minorId`는 0부터 시작한다. 따라서 metadata factory에서
`commitMajorId > 0`, `commitMinorId >= 0`을 `bluetape4k-core` validation
helper로 검증한다.

### 검증

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Result: 184 tests passed.

### 다음 작업자를 위한 규칙

transport metadata가 JaVers object에서 파생되는 값을 public factory로도
받는다면, upstream object가 보장하는 생성 규칙을 factory validation과
regression test에 같이 고정한다.
