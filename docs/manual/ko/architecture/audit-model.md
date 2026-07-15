# 감사 모델

JaVers 감사 기록은 업무 테이블을 한 번 더 복사한 데이터가 아닙니다. commit은 한 번의 감사 작업을 설명하고, snapshot은 그 커밋 시점의 객체 상태를 담습니다. change는 두 상태의 차이를 나타내며, shadow는 snapshot으로 과거 객체 모양을 다시 만듭니다.

## Commit과 스냅샷

`javers.commit(author, object, properties)`를 호출하면 커밋 metadata와 `CdoSnapshot`이 생깁니다. metadata에는 커밋 ID, 작성자, 시각, 문자열 property가 들어갑니다. [`CommitMetadataExtensions.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/commit/CommitMetadataExtensions.kt)는 커밋 ID의 major/minor 쌍과 epoch millisecond 시각을 제공합니다.

snapshot에는 `GlobalId`, 버전, state, 변경된 프로퍼티 이름, 유형, 커밋 metadata가 들어갑니다. `loadSnapshots`는 같은 GlobalId의 snapshot을 최신순으로 돌려줍니다. 다만 일반 JQL 조회도 내부에서 모든 key와 snapshot을 메모리에 올린 뒤 걸러냅니다. key가 10,000개를 넘으면 경고하지만 SQL 조건으로 밀어 넣지는 않습니다.

## Change와 shadow

change는 “무엇이 달라졌나”에 답하고, shadow는 “그때 객체가 어떤 모습이었나”에 답합니다. shadow는 현재 entity가 아닙니다. [`SnapshotToShadowTest.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/test/kotlin/io/bluetape4k/javers/SnapshotToShadowTest.kt)가 이 복원을 검증합니다. [`ShadowProvider.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/ShadowProvider.kt)는 JaVers 내부 `typeMapper`를 reflection으로 읽으므로 JaVers 내부 구조가 바뀌면 `IllegalStateException`이 날 수 있습니다.

## Codec과 저장소 계약

`JaversCodec<T>`는 JaVers `JsonObject`를 저장 형식으로 바꿉니다. 문자열, 압축 문자열, 바이너리, 압축 바이너리, map codec은 [`JaversCodecs.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/javers-core/src/main/kotlin/io/bluetape4k/javers/codecs/JaversCodecs.kt)에 있습니다. decode 실패를 `null`로 돌려주는 codec도 있어 손상된 snapshot이 조회 결과에서 빠질 수 있습니다. 다음은 [저장소 조합](repository-composition.md)입니다.
