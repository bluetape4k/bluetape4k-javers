# Changelog

All notable changes to `bluetape4k-javers` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-17

### Added

- Root README hero image plus refreshed purpose, feature, and Mermaid architecture documentation.
- GitHub Actions workflows for CI, nightly, snapshot, release, and code-quality checks ([PR #2](https://github.com/bluetape4k/bluetape4k-javers/pull/2)).
- `bluetape4k-javers-bom` BOM module for JaVers library consumers ([PR #10](https://github.com/bluetape4k/bluetape4k-javers/pull/10)).
- English and Korean README files for the JaVers BOM module ([PR #11](https://github.com/bluetape4k/bluetape4k-javers/pull/11)).
- JaVers implementation backlog captured in repository docs ([PR #12](https://github.com/bluetape4k/bluetape4k-javers/pull/12)).
- `JCacheCommitTest` covering `JCacheCdoSnapshotRepository` with a Caffeine JCache manager ([#46][i46], [PR #49][pr49]).
- `KafkaCdoSnapshotRepositoryTest`: `saveSnapshot propagates RuntimeException when Kafka publish fails` test verifying error-propagation contract ([#46][i46], [PR #49][pr49]).

### Changed

- Updated WIP snapshot from current assigned GitHub issues and refreshed agent guidance.
- Dependency governance, compatibility guard, Nightly lane, and Kover policy maintenance landed through PR #14 through PR #24.
- CI uses path filtering and retry configuration ([PR #8](https://github.com/bluetape4k/bluetape4k-javers/pull/8)).
- Test code migrated from Kluent to `bluetape4k-assertions` via `bluetape4k-junit5` ([PR #9](https://github.com/bluetape4k/bluetape4k-javers/pull/9)).
- `DebugDispacher` renamed to `DebugDispatcher` (typo fix, pre-1.0 API cleanup) ([#41][i41], [PR #47][pr47]).
- `EntityEnvelop` renamed to `EntityEnvelope` (typo fix, pre-1.0 API cleanup) ([#42][i42], [PR #47][pr47]).
- `CompressableStringJaversCodec` / `CompressableBinaryJaversCodec` renamed to `CompressibleStringJaversCodec` / `CompressibleBinaryJaversCodec` (typo fix, pre-1.0 API cleanup) ([#43][i43], [PR #47][pr47]).

### Fixed

- `AbstractCdoSnapshotRepository.saveSnapshot()` now propagates exceptions instead of silently swallowing them ([#33][i33], [PR #47][pr47]).
- `AbstractCdoSnapshotRepository.encode()` replaced unsafe `!!` on `jsonConverter` with `requireNotNull` and a descriptive error message ([#34][i34], [PR #47][pr47]).
- `ShadowProvider` reflection field lookup now uses `error()` with a descriptive message instead of `!!` ([#35][i35], [PR #47][pr47]).
- `KafkaCdoSnapshotRepository` publish now enforces a 30-second timeout instead of blocking indefinitely ([#36][i36], [PR #48][pr48]).
- `AbstractCdoSnapshotRepository.head` marked `@Volatile` to prevent stale reads across threads ([#37][i37], [PR #48][pr48]).
- `CaffeineCdoSnapshotRepository` and `Cache2KCdoSnapshotRepository` `loadSnapshots()` now hold the write lock during cache reads to prevent torn reads under concurrent access ([#38][i38], [PR #48][pr48]).
- `LettuceCdoSnapshotRepository` MULTI/EXEC transactions now use a dedicated connection (`writeCommands`) to prevent shared-connection races under concurrent writes ([#39][i39], [PR #48][pr48]).
- `KafkaCdoSnapshotRepository` read-path methods (`getKeys`, `contains`, `getSeq`, `getSnapshotSize`, `loadSnapshots`) now emit a `WARN` log on every call to make the write-only contract visible ([#40][i40], [PR #48][pr48]).
- `AbstractCdoSnapshotRepository.getAll()` now logs a warning and short-circuits when the key set exceeds 10 000 entries to prevent unbounded heap allocation ([#44][i44], [PR #48][pr48]).
- `AbstractJaversCommitTest` no-op `isEmpty()` assertions replaced with `shouldBeEqualTo emptyList()` so test failures surface correctly ([#45][i45], [PR #49][pr49]).
- `KafkaCdoSnapshotRepositoryTest` disabled inherited `@ShallowReference` snapshot tests that cannot pass on a write-only Kafka repository ([#46][i46], [PR #49][pr49]).

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
