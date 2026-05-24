# 2026-05-24 — JaVers Redis head restore

## Context

Issue #62 found that persistent JaVers repositories could rebuild against
existing Redis snapshots while `headId` stayed `null`, because
`AbstractCdoSnapshotRepository` only returned the process-local `head` field.
Issue #76 required restart/rebuild coverage for Redis and Kafka repositories.

## Decision

Add a lazy `restoreHeadId()` hook to `AbstractCdoSnapshotRepository`. Redis
repositories restore the latest head from their persisted `CommitId -> sequence`
metadata by selecting the highest sequence. Kafka remains explicitly write-only,
so a rebuilt Kafka repository does not restore head metadata.

Redisson sequence maps need separate key/value codecs for `readAllMap()`. A
plain `LongCodec` encodes keys through `StringCodec`, so existing string commit
id key bytes remain compatible, but `BaseCodec.getMapKeyDecoder()` also points
to `LongCodec.getValueDecoder()` and tries to decode keys such as `1.00` as
Long values. Use `CompositeCodec(StringCodec(), LongCodec())` for string commit
id keys and long sequence values.

## Outcome

Lettuce and Redisson now restore `headId` after repository rebuilds without
flushing Redis, then continue commits with the restored head. Kafka has a
contract test documenting the unsupported restore path.

## Verification

- `./gradlew :javers-persistence-redis:test --tests '*LettuceJaversCommitTest*' --tests '*RedissonJaversCommitTest*' --no-daemon` — 6 passing.
- `./gradlew :javers-persistence-kafka:test --tests '*KafkaCdoSnapshotRepositoryTest*' --no-daemon` — 4 passing.
- `./gradlew :javers-core:test --no-daemon` — 167 passing.
- `git diff --check` — clean.

## Future Agents

When adding persistent repositories based on `AbstractCdoSnapshotRepository`,
persist enough commit sequence metadata to implement `restoreHeadId()`. For
write-only sinks, add an explicit test that documents why restart restore is not
supported.
