# Issue #3 — javers-exposed 설계

## 목표

`javers-exposed` 모듈을 추가해 JaVers `JaversRepository` SPI를 Exposed JDBC DSL 위에서 동작하게 한다.
JPA/Hibernate 없이 Exposed transaction 안에서 CDO snapshot audit 데이터를 저장하고 조회한다.

## 근거

- GitHub issue #3: Exposed JDBC 기반 `ExposedCdoSnapshotRepository` 구현.
- JaVers 7.11.x 공식 repository 문서: repository는 commit metadata와 snapshot JSON을 저장하고, diff는 조회 시 snapshot 비교로 재계산한다.
- JaVers 7.11.0 source jar: `JaversRepository`는 `persist`, `getLatest`, `getSnapshots`, `getStateHistory`, `getHeadId`, `ensureSchema`, `setJsonConverter`를 요구한다.
- 현재 repo source: `AbstractCdoSnapshotRepository`는 common JaVers SPI behavior, codec-based JSON encode/decode, query filtering, lazy `loadHeadId()` restoration hook을 이미 구현한다.
- Prior lessons: persistent repository는 rebuild 후 `headId`를 restore할 수 있도록 충분한 sequence metadata를 persist해야 한다.
- Exposed 1.3.0 docs/source: `org.jetbrains.exposed.v1.*` import, top-level `eq`, `SchemaUtils`, `transaction {}`를 사용한다.

## 범위

### 새 모듈

- `javers-exposed/`
- Artifact: `io.github.bluetape4k.javers:javers-exposed`
- Package root: `io.bluetape4k.javers.persistence.exposed`

### Public API

- `ExposedCdoSnapshotRepository`
  - `AbstractCdoSnapshotRepository<String>`을 확장한다.
  - 기본적으로 `JaversCodecs.String`을 사용한다.
  - 명시적 transaction routing을 위해 optional Exposed `Database`를 받는다.
  - Exposed table을 생성하도록 `ensureSchema()`를 override한다.
  - Persisted commit sequence metadata를 사용해 `loadHeadId()`를 override한다.
- Table object:
  - `CdoSnapshotTable`
  - `CommitTable`

### Schema

`CdoSnapshotTable : LongIdTable("javers_snapshot")`

- `global_id: varchar(200)`
- `commit_id: varchar(50)`
- `version: long`
- `type: varchar(50)`
- `state: text`
  - State map만이 아니라 JaVers가 생성한 full encoded `CdoSnapshot` JSON payload를 저장한다.
  - 이를 통해 `JsonConverter.fromJson(..., CdoSnapshot::class.java)`로 lossless reconstruction을 유지한다.
- `changed_properties: text`
- `managed_type: varchar(200)`

`CommitTable : LongIdTable("javers_commit")`

- `commit_id: varchar(50).uniqueIndex()`
- `author: varchar(200)`
- `commit_date: datetime`
- `commit_date_instant: varchar(64).nullable()`
- `properties: text`
- `sequence: long`
  - Repository `headId` rebuild contract를 보존하기 위해 issue sketch보다 추가한다.

## 동작 계약

- `persist(commit)`은 `AbstractCdoSnapshotRepository`에서 inherited된 동작을 유지한다.
- `saveSnapshot(snapshot)`은 snapshot마다 row 하나를 insert하고 commit id마다 commit metadata를 한 번 upsert/insert한다.
- `loadSnapshots(globalId)`는 하나의 global id에 대한 snapshot을 version/commit sequence 기준 reverse chronological order로 반환한다.
- `getLatest(globalId)`는 inherited `loadSnapshots(globalId).firstOrNull()`를 통해 동작한다.
- 첫 구현의 `getSnapshots(queryParams)`와 JQL path는 loaded snapshot에 대한 inherited filtering을 사용한다.
- `getSnapshots(snapshotIdentifiers)`는 inherited identifier lookup 및 reverse-order indexing을 통해 동작한다.
- `ensureSchema()`는 `SchemaUtils.create(CdoSnapshotTable, CommitTable)`을 호출한다.
- `loadHeadId()`는 저장된 sequence가 가장 높은 commit id를 반환한다.

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

- `settings.gradle.kts`에 module을 등록한다.
- Imported dependency catalog에 없을 때만 module dependency alias를 local로 추가한다.
- Root `README.md`와 `README.ko.md`의 module table 및 build command를 갱신한다.
- `javers-persistence-options` diagram을 planned Exposed에서 implemented Exposed로 갱신한다.
- English 및 Korean module README를 추가한다.
- `javers-exposed`용 CI 및 Nightly filter/job/coverage aggregation을 갱신한다.
- Workflow edit 후 `actionlint`를 실행한다.

## 비범위

- Spring Boot auto-configuration.
- R2DBC repository.
- Query pushdown optimization for every `QueryParams` filter.
- Migration tool integration.

## 위험과 완화

- Full snapshot JSON vs issue wording `state`: column contract를 문서화하고 visibility 및 future pushdown을 위해 metadata column을 유지한다.
- Query performance: inherited filtering은 snapshot을 memory에 load한다. Base class warning을 유지하고 first-release scope로 문서화한다.
- Transaction ownership: caller가 database를 전달할 수 있도록 Exposed `transaction(database)`를 default로 사용한다. Manual JDBC commit/rollback은 피한다.
- Head restoration: `CommitTable`에 sequence를 persist하고 완료 전에 rebuild를 test한다.

## 검토 노트

### Local Step 2-R / 3-R 검토

| 우선순위 | Finding | 결정 |
|---|---|---|
| P1 | Lossless `CdoSnapshot` reconstruction에는 full snapshot JSON payload가 필요하다. `state`만 저장하면 `JaversRepository` semantics를 만족하지 못한다. | 수용. `state` column은 full encoded snapshot JSON을 저장하고 metadata column은 secondary query/index field다. |
| P1 | Prior Redis lesson에 따라 persistent repository rebuild는 `headId`를 restore해야 한다. | 수용. `CommitTable.sequence`는 schema 일부이며 explicit rebuild test가 있다. |
| P2 | Inherited filtering이 모든 snapshot을 load하므로 첫 구현은 broad JQL query에서 O(N)일 수 있다. | First-release scope로 수용한다. Non-goal 및 future pushdown target으로 문서화한다. |
| P2 | CI/Nightly는 local Gradle test만이 아니라 새 module도 포함해야 한다. | Plan에 수용한다. |

### 과거 외부 CLI 검토 시도

이 작업 중 historical artifact가 `.omx/artifacts` 아래 기록됐다.

당시 요청에 따라 external CLI review를 5-minute timeout으로 실행했다. Usable output 없이 timeout됐다(`rc=142`). 현재 workflow policy는 local/native 7-tier review를 required gate로 사용하므로, 이 note는 historical evidence로만 보존하고 active blocker로 취급하지 않는다.
