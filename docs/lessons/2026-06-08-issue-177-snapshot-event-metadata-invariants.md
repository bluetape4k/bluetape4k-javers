# 교훈 - 이슈 177 스냅샷 이벤트 메타데이터 불변 조건

**관련 이슈**: #177
**영향 모듈**: `javers-core`

## L1: 전송 메타데이터 팩터리는 JaVers 생성 규칙을 다시 고정한다

### 문제

`CdoSnapshotEventMetadata.from(snapshot, ...)`는 JaVers가 만든 `CommitId`에서
유효한 `majorId`와 `minorId`를 복사하지만, 공개 companion 객체의 팩터리는 외부
어댑터나 테스트가 직접 호출할 수 있다. 기존 팩터리는 필수 문자열,
`snapshotVersion`, `repositorySequence`만 검증했고 커밋 구성 요소의 숫자
범위는 그대로 통과시켰다.

### 결정

JaVers 7.11.0 소스 기준으로 생성된 커밋 ID는 `majorId`가 1부터
시작하고, `minorId`는 0부터 시작한다. 따라서 메타데이터 팩터리에서
`commitMajorId > 0`, `commitMinorId >= 0`을 `bluetape4k-core` 검증
도우미로 확인한다.

### 검증

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- 결과: 테스트 184개 통과.

### 다음 작업자를 위한 규칙

전송 메타데이터가 JaVers 객체에서 파생되는 값을 공개 팩터리로도
받는다면, 상위 객체가 보장하는 생성 규칙을 팩터리 검증과
회귀 테스트에 같이 고정한다.
