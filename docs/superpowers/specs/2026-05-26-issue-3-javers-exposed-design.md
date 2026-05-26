# Issue #3 — javers-exposed Design

## 목표

`javers-exposed` 모듈을 추가해 JaVers `JaversRepository` SPI를 Exposed JDBC DSL 위에서 동작하게 한다.
JPA/Hibernate 없이 Exposed transaction 안에서 CDO snapshot audit 데이터를 저장하고 조회한다.

## 근거

- GitHub issue #3: Exposed JDBC 기반 `ExposedCdoSnapshotRepository` 구현.
- JaVers 7.11.x 공식 repository 문서: repository는 commit metadata와 snapshot JSON을 저장하고, diff는 조회 시 snapshot 비교로 재계산한다.
- JaVers 7.11.0 source jar: `JaversRepository`는 `persist`, `getLatest`, `getSnapshots`, `getStateHistory`, `getHeadId`, `ensureSchema`, `setJsonConverter`를 요구한다.
- Current repo source: `AbstractCdoSnapshotRepository` already implements common JaVers SPI behavior, codec-based JSON encode/decode, query filtering, and lazy `loadHeadId()` restoration hook.
- Prior lessons: persistent repositories must persist enough sequence metadata to restore `headId` after rebuild.
- Exposed 1.3.0 docs/source: use `org.jetbrains.exposed.v1.*` imports, top-level `eq`, `SchemaUtils`, and `transaction {}`.

## 범위

### 새 모듈

- `javers-exposed/`
- Artifact: `io.github.bluetape4k.javers:javers-exposed`
- Package root: `io.bluetape4k.javers.persistence.exposed`

### Public API

- `ExposedCdoSnapshotRepository`
  - Extends `AbstractCdoSnapshotRepository<String>`.
  - Uses `JaversCodecs.String` by default.
  - Accepts optional Exposed `Database` for explicit transaction routing.
  - Overrides `ensureSchema()` to create Exposed tables.
  - Overrides `loadHeadId()` using persisted commit sequence metadata.
- Table objects:
  - `CdoSnapshotTable`
  - `CommitTable`

### Schema

`CdoSnapshotTable : LongIdTable("javers_snapshot")`

- `global_id: varchar(200)`
- `commit_id: varchar(50)`
- `version: long`
- `type: varchar(50)`
- `state: text`
  - Stores the full encoded `CdoSnapshot` JSON payload produced by JaVers, not only the state map.
  - This keeps lossless reconstruction via `JsonConverter.fromJson(..., CdoSnapshot::class.java)`.
- `changed_properties: text`
- `managed_type: varchar(200)`

`CommitTable : LongIdTable("javers_commit")`

- `commit_id: varchar(50).uniqueIndex()`
- `author: varchar(200)`
- `commit_date: datetime`
- `commit_date_instant: varchar(64).nullable()`
- `properties: text`
- `sequence: long`
  - Added beyond the issue sketch to preserve the repository `headId` rebuild contract.

## 동작 계약

- `persist(commit)` remains inherited from `AbstractCdoSnapshotRepository`.
- `saveSnapshot(snapshot)` inserts one row per snapshot and upserts/inserts the commit metadata once per commit id.
- `loadSnapshots(globalId)` returns snapshots for one global id in reverse chronological order by version/commit sequence.
- `getLatest(globalId)` works through inherited `loadSnapshots(globalId).firstOrNull()`.
- `getSnapshots(queryParams)` and JQL paths use inherited filtering over loaded snapshots for the first implementation.
- `getSnapshots(snapshotIdentifiers)` works through inherited identifier lookup and reverse-order indexing.
- `ensureSchema()` calls `SchemaUtils.create(CdoSnapshotTable, CommitTable)`.
- `loadHeadId()` returns the commit id with the highest stored sequence.

## 테스트 요구

- H2 in-memory integration tests:
  - schema creation
  - commit and latest snapshot
  - state history / JQL basics
  - rebuild restores head id and allows next commit
  - snapshot identifier lookup
- PostgreSQL Testcontainers integration test:
  - schema + commit/latest smoke
- MySQL 8 Testcontainers integration test:
  - schema + commit/latest smoke

Testcontainers-backed tests must run serially in one Gradle invocation.

## 문서/CI

- Register module in `settings.gradle.kts`.
- Add module dependency aliases locally only when missing from the imported dependency catalog.
- Update root `README.md` and `README.ko.md` module table and build commands.
- Update `javers-persistence-options` diagram from planned Exposed to implemented Exposed.
- Add module README in English and Korean.
- Update CI and Nightly filters/jobs/coverage aggregation for `javers-exposed`.
- Run `actionlint` after workflow edits.

## 비범위

- Spring Boot auto-configuration.
- R2DBC repository.
- Query pushdown optimization for every `QueryParams` filter.
- Migration tool integration.

## 위험과 완화

- Full snapshot JSON vs issue wording `state`: document the column contract and keep metadata columns for visibility and future pushdown.
- Query performance: inherited filtering loads snapshots in memory. Keep warning from base class and document first-release scope.
- Transaction ownership: default to Exposed `transaction(database)`, allowing callers to pass a database. Avoid manual JDBC commit/rollback.
- Head restoration: persist sequence in `CommitTable`; test rebuild before completion.

## Review Notes

### Local Step 2-R / 3-R Review

| Priority | Finding | Decision |
|---|---|---|
| P1 | A full snapshot JSON payload is required for lossless `CdoSnapshot` reconstruction; storing only `state` would not satisfy `JaversRepository` semantics. | Accepted. The `state` column stores full encoded snapshot JSON and metadata columns are secondary query/index fields. |
| P1 | Persistent repository rebuild must restore `headId`, based on prior Redis lessons. | Accepted. `CommitTable.sequence` is part of the schema and has explicit rebuild tests. |
| P2 | First implementation may be O(N) for broad JQL queries because inherited filtering loads all snapshots. | Accepted as first-release scope; documented as non-goal and future pushdown target. |
| P2 | CI/Nightly must include the new module, not only local Gradle tests. | Accepted in plan. |

### Claude Code Advisor

Artifact: `.omx/artifacts/claude-issue-3-spec-plan-5min-20260526210042.md`

The Claude Code CLI advisor was run with a 5-minute timeout as requested. It
timed out with no usable output (`rc=142`), so it cannot close the external
advisor gate. Work continues with local review evidence; final DoD must report
this validation gap.
