# 변경 이력

`bluetape4k-javers`의 중요한 변경 사항을 이 문서에 기록한다.

형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)를 따른다.
이 프로젝트는 [Semantic Versioning](https://semver.org/spec/v2.0.0.html)을 따른다.

## [미공개]

### 추가

- `javers-spring-boot4-autoconfigure`의 schema ownership과 상충 flag 조합을
  fail-fast로 검증하고 현재 module inventory와 Spring Boot 4 예제를 정렬했다
  ([#338](https://github.com/bluetape4k/bluetape4k-javers/issues/338),
  [PR #349](https://github.com/bluetape4k/bluetape4k-javers/pull/349)).
- benchmark teardown과 CI 첫 시도 실패를 재현 가능한 JSON receipt로 남기는
  fail-closed evidence 경로를 추가했다
  ([#339](https://github.com/bluetape4k/bluetape4k-javers/issues/339),
  [PR #352](https://github.com/bluetape4k/bluetape4k-javers/pull/352),
  [#342](https://github.com/bluetape4k/bluetape4k-javers/issues/342),
  [PR #353](https://github.com/bluetape4k/bluetape4k-javers/pull/353)).

### 변경

- Redis repository head가 손상되거나 되감긴 경우 audit history를 조용히
  축소하지 않도록 fail-closed 검증을 적용했다
  ([#334](https://github.com/bluetape4k/bluetape4k-javers/issues/334),
  [PR #345](https://github.com/bluetape4k/bluetape4k-javers/pull/345)).
- 배포 library의 Kotlin public ABI baseline을 Kotlin Gradle Plugin built-in
  validator로 고정했다
  ([#341](https://github.com/bluetape4k/bluetape4k-javers/issues/341),
  [PR #346](https://github.com/bluetape4k/bluetape4k-javers/pull/346)).
- core codec cancellation, weak-key cache lifecycle, projection commit 경계와
  Ktor/Spring 예제의 bluetape4k 공용 helper 사용을 정리했다
  ([#333](https://github.com/bluetape4k/bluetape4k-javers/issues/333),
  [PR #347](https://github.com/bluetape4k/bluetape4k-javers/pull/347),
  [#335](https://github.com/bluetape4k/bluetape4k-javers/issues/335),
  [PR #348](https://github.com/bluetape4k/bluetape4k-javers/pull/348),
  [#336](https://github.com/bluetape4k/bluetape4k-javers/issues/336),
  [PR #350](https://github.com/bluetape4k/bluetape4k-javers/pull/350),
  [#337](https://github.com/bluetape4k/bluetape4k-javers/issues/337),
  [PR #351](https://github.com/bluetape4k/bluetape4k-javers/pull/351)).
- 테스트 assertion을 `bluetape4k-assertions`의 의도 중심 matcher로 교체하고
  예외·collection·null 계약을 직접 표현했다
  ([#340](https://github.com/bluetape4k/bluetape4k-javers/issues/340),
  [PR #354](https://github.com/bluetape4k/bluetape4k-javers/pull/354)).

## [0.3.0] - 2026-08-06

### 추가

- Exposed, Redis, Kafka repository backend 전반에서 Spring Boot 4 조건부
  JaVers auto-configuration을 제공하는 `javers-spring-boot4-autoconfigure`를
  추가했다 ([#104](https://github.com/bluetape4k/bluetape4k-javers/issues/104)).
- `benchmark/javers-exposed-benchmark`를 위한 module-local benchmark 문서와
  CI/Nightly smoke coverage를 추가했다
  ([#195](https://github.com/bluetape4k/bluetape4k-javers/issues/195)).

### 변경

- `0.2.1` release-train patch 이후 `0.3.0` 개발 라인을 열었다.
- local `bluetape4k-bom` 참조를 `1.11.0-SNAPSHOT`에 맞췄다.
- 한국어 release-facing changelog에서 Keep a Changelog `Fixed` category를
  `버그 수정`으로 표준화했다 ([#289](https://github.com/bluetape4k/bluetape4k-javers/issues/289)).

## [0.2.1] - 2026-06-01

### 변경

- `javers-exposed`가 API scope로 BOM platform을 노출하지 않고,
  implementation scope platform import를 통해 release-train
  `bluetape4k-exposed-bom` 라인을 소비하도록 변경했다.
- release workflow 정렬을 위해 기본 bluetape4k dependencies catalog ref를
  `catalog/2026-06-01-00`으로 갱신했다.

## [0.2.0] - 2026-05-27

### 추가

- schema management와 repository contract test를 갖춘 Exposed JDBC-backed
  JaVers CDO snapshot repository인 `javers-exposed`를 추가했다
  ([#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3), [PR #86](https://github.com/bluetape4k/bluetape4k-javers/pull/86)).
- JaVers-friendly DDD model을 위한 aggregate root와 domain event helper인
  `javers-ddd`를 추가했다
  ([#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4), [PR #87](https://github.com/bluetape4k/bluetape4k-javers/pull/87)).
- command-side persistence, Kafka event, Redis projection, 측정된 Envers 비교
  note를 포함한 CQRS/Event Sourcing 예제 `examples/javers-exposed-ddd`를
  추가했다 ([#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5), [#88](https://github.com/bluetape4k/bluetape4k-javers/issues/88), [#89](https://github.com/bluetape4k/bluetape4k-javers/issues/89), [#90](https://github.com/bluetape4k/bluetape4k-javers/issues/90), [PR #91](https://github.com/bluetape4k/bluetape4k-javers/pull/91), [PR #92](https://github.com/bluetape4k/bluetape4k-javers/pull/92), [PR #93](https://github.com/bluetape4k/bluetape4k-javers/pull/93)).
- repository/module 분리를 명확히 하기 위해 root README에 persistence
  relationship diagram을 추가했다
  ([#77](https://github.com/bluetape4k/bluetape4k-javers/issues/77), [PR #85](https://github.com/bluetape4k/bluetape4k-javers/pull/85)).

### 변경

- 기본 bluetape4k dependencies catalog 참조를 정렬하고 bluetape4k projects
  BOM 1.9.2 라인을 소비하도록 했다
  ([PR #81](https://github.com/bluetape4k/bluetape4k-javers/pull/81), [PR #83](https://github.com/bluetape4k/bluetape4k-javers/pull/83)).
- `javers-exposed` database smoke coverage를 공유
  `bluetape4k-exposed-jdbc-tests` H2/PostgreSQL/MySQL_V8 matrix로 옮겼다
  ([#95](https://github.com/bluetape4k/bluetape4k-javers/issues/95), [PR #96](https://github.com/bluetape4k/bluetape4k-javers/pull/96)).

### 버그 수정

- Redis rebuild scenario를 포함한 persistent backend에서 persistent JaVers
  repository head recovery를 복구했다
  ([PR #78](https://github.com/bluetape4k/bluetape4k-javers/pull/78), [PR #80](https://github.com/bluetape4k/bluetape4k-javers/pull/80)).
- 필수 database matrix가 H2, PostgreSQL, MySQL_V8 전반에서 portable하게 유지되도록
  Exposed repository commit metadata 경로에서 dialect-specific `insertIgnore`
  의존성을 제거했다
  ([#95](https://github.com/bluetape4k/bluetape4k-javers/issues/95), [PR #96](https://github.com/bluetape4k/bluetape4k-javers/pull/96)).

## [0.1.1] - 2026-05-22

### 변경

- 0.1.0 tag 이후 0.1.1 release line을 열고 준비했다
  ([#60](https://github.com/bluetape4k/bluetape4k-javers/issues/60)).
- live GitHub issue state에서 WIP를 갱신하고 0.2.0 feature lane을 0.1.1 release
  gate와 분리해 유지했다
  ([#63](https://github.com/bluetape4k/bluetape4k-javers/issues/63)).
- 0.1.1 release line이 `io.github.bluetape4k:bluetape4k-bom:1.9.0`을
  소비하도록 준비했다.
- 0.1.0 이후 README overview visual과 dependency catalog maintenance를 갱신했다.

## [0.1.0] - 2026-05-17

### 추가

- root README hero image와 갱신된 purpose, feature, Mermaid architecture 문서를
  추가했다.
- CI, nightly, snapshot, release, code-quality check를 위한 GitHub Actions
  workflow를 추가했다
  ([PR #2](https://github.com/bluetape4k/bluetape4k-javers/pull/2)).
- JaVers library consumer를 위한 `bluetape4k-javers-bom` BOM module을 추가했다
  ([PR #10](https://github.com/bluetape4k/bluetape4k-javers/pull/10)).
- JaVers BOM module의 영어/한국어 README 파일을 추가했다
  ([PR #11](https://github.com/bluetape4k/bluetape4k-javers/pull/11)).
- JaVers implementation backlog를 repository docs에 기록했다
  ([PR #12](https://github.com/bluetape4k/bluetape4k-javers/pull/12)).
- Caffeine JCache manager로 `JCacheCdoSnapshotRepository`를 검증하는
  `JCacheCommitTest`를 추가했다 ([#46][i46], [PR #49][pr49]).
- error-propagation contract를 검증하는
  `KafkaCdoSnapshotRepositoryTest`의 `saveSnapshot propagates RuntimeException
  when Kafka publish fails` test를 추가했다 ([#46][i46], [PR #49][pr49]).

### 변경

- 현재 assigned GitHub issue에서 WIP snapshot을 갱신하고 agent guidance를 갱신했다.
- Dependency governance, compatibility guard, Nightly lane, Kover policy
  maintenance가 PR #14부터 PR #24까지 반영됐다.
- CI가 path filtering과 retry configuration을 사용하도록 했다
  ([PR #8](https://github.com/bluetape4k/bluetape4k-javers/pull/8)).
- test code를 Kluent에서 `bluetape4k-junit5`를 통한 `bluetape4k-assertions`로
  migration했다 ([PR #9](https://github.com/bluetape4k/bluetape4k-javers/pull/9)).
- `DebugDispacher`를 `DebugDispatcher`로 이름 변경했다
  (typo fix, pre-1.0 API cleanup) ([#41][i41], [PR #47][pr47]).
- `EntityEnvelop`를 `EntityEnvelope`로 이름 변경했다
  (typo fix, pre-1.0 API cleanup) ([#42][i42], [PR #47][pr47]).
- `CompressableStringJaversCodec` / `CompressableBinaryJaversCodec`를
  `CompressibleStringJaversCodec` / `CompressibleBinaryJaversCodec`로 이름
  변경했다 (typo fix, pre-1.0 API cleanup) ([#43][i43], [PR #47][pr47]).

### 버그 수정

- `AbstractCdoSnapshotRepository.saveSnapshot()`이 예외를 조용히 삼키지 않고
  전파하도록 수정했다 ([#33][i33], [PR #47][pr47]).
- `AbstractCdoSnapshotRepository.encode()`가 `jsonConverter`에 대한 unsafe `!!`
  대신 `requireNotNull`과 설명적인 error message를 사용하도록 수정했다
  ([#34][i34], [PR #47][pr47]).
- `ShadowProvider` reflection field lookup이 `!!` 대신 설명적인 message가 있는
  `error()`를 사용하도록 수정했다 ([#35][i35], [PR #47][pr47]).
- `KafkaCdoSnapshotRepository` publish가 무기한 block되지 않고 30초 timeout을
  강제하도록 수정했다 ([#36][i36], [PR #48][pr48]).
- thread 간 stale read를 막기 위해 `AbstractCdoSnapshotRepository.head`에
  `@Volatile`을 표시했다 ([#37][i37], [PR #48][pr48]).
- concurrent access에서 torn read를 막기 위해 `CaffeineCdoSnapshotRepository`와
  `Cache2KCdoSnapshotRepository`의 `loadSnapshots()`가 cache read 중 write lock을
  잡도록 수정했다 ([#38][i38], [PR #48][pr48]).
- concurrent write 중 shared-connection race를 막기 위해
  `LettuceCdoSnapshotRepository` MULTI/EXEC transaction이 dedicated connection
  (`writeCommands`)을 사용하도록 수정했다 ([#39][i39], [PR #48][pr48]).
- write-only contract가 드러나도록 `KafkaCdoSnapshotRepository` read-path method
  (`getKeys`, `contains`, `getSeq`, `getSnapshotSize`, `loadSnapshots`)가 호출될
  때마다 `WARN` log를 남기도록 수정했다 ([#40][i40], [PR #48][pr48]).
- unbounded heap allocation을 막기 위해 `AbstractCdoSnapshotRepository.getAll()`이
  key set이 10 000개를 넘으면 warning을 남기고 short-circuit하도록 수정했다
  ([#44][i44], [PR #48][pr48]).
- test failure가 제대로 드러나도록 `AbstractJaversCommitTest`의 no-op
  `isEmpty()` assertion을 `shouldBeEqualTo emptyList()`로 교체했다
  ([#45][i45], [PR #49][pr49]).
- write-only Kafka repository에서 통과할 수 없는 inherited `@ShallowReference`
  snapshot test를 `KafkaCdoSnapshotRepositoryTest`에서 비활성화했다
  ([#46][i46], [PR #49][pr49]).

[i33]: https://github.com/bluetape4k/bluetape4k-javers/issues/33
[i34]: https://github.com/bluetape4k/bluetape4k-javers/issues/34
[i35]: https://github.com/bluetape4k/bluetape4k-javers/issues/35
[i36]: https://github.com/bluetape4k/bluetape4k-javers/issues/36
[i37]: https://github.com/bluetape4k/bluetape4k-javers/issues/37
[i38]: https://github.com/bluetape4k/bluetape4k-javers/issues/38
[i39]: https://github.com/bluetape4k/bluetape4k-javers/issues/39
[i40]: https://github.com/bluetape4k/bluetape4k-javers/issues/40
[i41]: https://github.com/bluetape4k/bluetape4k-javers/issues/41
[i42]: https://github.com/bluetape4k/bluetape4k-javers/issues/42
[i43]: https://github.com/bluetape4k/bluetape4k-javers/issues/43
[i44]: https://github.com/bluetape4k/bluetape4k-javers/issues/44
[i45]: https://github.com/bluetape4k/bluetape4k-javers/issues/45
[i46]: https://github.com/bluetape4k/bluetape4k-javers/issues/46
[pr47]: https://github.com/bluetape4k/bluetape4k-javers/pull/47
[pr48]: https://github.com/bluetape4k/bluetape4k-javers/pull/48
[pr49]: https://github.com/bluetape4k/bluetape4k-javers/pull/49
