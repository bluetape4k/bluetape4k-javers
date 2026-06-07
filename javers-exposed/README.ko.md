# Module bluetape4k-javers-exposed

[English](./README.md) | 한국어

[JaVers](https://javers.org) CDO snapshot을 위한 Exposed JDBC persistence 모듈입니다.
`ExposedCdoSnapshotRepository`는 JetBrains Exposed로 관리되는 SQL table에
JaVers commit과 encoded snapshot을 저장하는 `AbstractCdoSnapshotRepository`
구현체입니다.

## 기능

- JaVers commit마다 `javers_commit` row를 저장합니다.
- CDO snapshot version마다 `javers_snapshot` row를 저장합니다.
- 기본 table명이 기존 schema나 tenant layout과 충돌할 때 repository 단위
  table명을 지정할 수 있습니다.
- JaVers JSON converter로 snapshot을 복원할 수 있도록 전체 encoded
  `CdoSnapshot` payload를 저장합니다.
- snapshot history 조회 hot path를 위해 `(global_id, version)` unique index를
  유지하고, repository head 복원을 위해 commit sequence index를 사용합니다.
- 일반적인 JaVers snapshot filter를 snapshot JSON decode 전에 SQL로 pushdown합니다.
- repository instance를 다시 만들 때 head commit id를 복원합니다.
- 명시적인 `EntityHook` subscription으로 Exposed DAO `Entity` lifecycle event를
  audit합니다.
- Exposed JDBC를 통해 H2, PostgreSQL, MySQL을 지원합니다.

## Architecture

![javers-exposed architecture](docs/images/readme-diagrams/javers-exposed-architecture-01.png)

`ExposedCdoSnapshotRepository`는 공통
`AbstractCdoSnapshotRepository`의 codec 및 query 동작을 확장하고, JaVers
commit metadata와 encoded `CdoSnapshot` row를 Exposed table에 매핑합니다.

## Persistence Flow

![javers-exposed persistence sequence](docs/images/readme-diagrams/javers-exposed-sequence-01.png)

쓰기 작업은 Exposed transaction 안에서 commit metadata를 필요할 때만
삽입하고 encoded snapshot payload를 저장합니다. 조회 시에는 snapshot
version 순서로 row를 읽고 decode하여 JaVers query에 돌려줍니다.

## 사용 예

```kotlin
val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/app",
    driver = "org.postgresql.Driver",
    user = "app",
    password = "secret",
)

val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .build()
```

migration 도구가 schema 생성을 담당하거나 기본 table명을 사용할 수 없을 때는
repository options를 사용합니다:

```kotlin
val options = ExposedCdoSnapshotRepositoryOptions(
    tableNames = ExposedJaversTableNames(
        commitTableName = "audit_commit",
        snapshotTableName = "audit_snapshot",
    ),
    createSchemaOnEnsure = false,
)

val repository = ExposedCdoSnapshotRepository(
    database = database,
    options = options,
)
```

`createSchemaOnEnsure = false`이면 `ensureSchema()`는 아무 작업도 하지
않습니다. snapshot을 쓰기 전에 애플리케이션이 Exposed `SchemaUtils`, Flyway,
Liquibase 같은 migration 도구로 매핑된 table을 생성해야 합니다.

## DAO EntityHook Audit

`ExposedJaversEntityHookSubscription`을 사용하면 각 DAO repository method마다
`javers.commit()`을 직접 호출하지 않고 Exposed DAO entity lifecycle event를
audit할 수 있습니다. 이 기능은 Exposed DAO `EntityHook` 전용입니다. raw
Exposed DSL write, 외부 database write, CDC stream은 감지하지 않습니다.

각 DAO entity class를 detached JaVers audit object로 매핑합니다:

```kotlin
data class AuditedCustomer(
    @Id val id: Int,
    val name: String,
)

class CustomerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CustomerEntity>(Customers)

    var name by Customers.name
}

val mapping = ExposedJaversEntityHookMapping(
    entityClass = CustomerEntity,
    auditType = AuditedCustomer::class.java,
    toAuditObject = { entity ->
        AuditedCustomer(
            id = entity.id.value,
            name = entity.name,
        )
    },
)

val subscription = ExposedJaversEntityHookSubscription.subscribe(
    javers = javers,
    mappings = listOf(mapping),
    authorProvider = { "system" },
    commitPropertiesProvider = { change ->
        mapOf("changeType" to change.changeType.name)
    },
)
```

application scope가 끝나면 subscription을 닫아 전역 hook을 해제합니다:

```kotlin
subscription.close()
```

생성/수정 DAO event는 flush된 entity state로 commit합니다. 삭제 DAO event는
row가 제거된 뒤 entity state를 다시 읽지 않고 `commitShallowDeleteById()`로
JaVers terminal snapshot을 만듭니다.

## Query 동작

`ExposedCdoSnapshotRepository`는 일반적인 JaVers snapshot query에서 full
repository scan을 피합니다:

- `getStateHistory(globalId, queryParams)`는 aggregate child value object
  query가 아닐 때 요청한 global id만 읽습니다.
- `getStateHistory(classes, queryParams)`는 aggregate child value object
  query가 아닐 때 저장된 managed type으로 먼저 필터링합니다.
- `getSnapshots(queryParams)`는 exact commit id, exact version, exact
  author, `LocalDateTime` commit date range, snapshot type, `skip`, `limit`을
  snapshot JSON decode 전에 SQL query로 pushdown합니다.

JaVers in-memory semantics가 필요한 query는 기존 공통 repository filtering
경로로 fallback합니다. 여기에는 aggregate child value object query,
changed-property filter, commit-property filter, author-like matching,
`Instant` commit date range, version range, `toCommitId`,
`snapshotQueryLimit`이 포함됩니다.

## Redis + Exposed 지연 시간 전략

Exposed가 database transaction을 소유하는 애플리케이션에서는
`javers-exposed`를 durable JaVers audit source of truth로 사용하세요. Redis
near-cache 또는 read/write-through 동작은 canonical `CdoSnapshot` row나
repository head metadata가 아니라 application read model과 projection에
적용하는 것이 안전합니다.

권장 재사용 경로는 기존 `bluetape4k-exposed` cache stack입니다:

- `exposed-cache`는 `CacheMode`, `CacheWriteMode`, local cache option,
  resilience option, 재사용 가능한 repository test fixture를 제공합니다.
- `exposed-jdbc-redisson`은 Redisson read-through, write-through,
  write-behind, near-cache repository를 제공합니다.
- `exposed-jdbc-lettuce`는 Lettuce read-through, write-through,
  write-behind repository를 제공합니다.

| 전략 | JaVers + Exposed에서 사용하기 좋은 대상 | 피해야 할 대상 |
|---|---|---|
| Cache-aside | audit history에서 재생성 가능한 query result 또는 projection. | canonical audit snapshot write. |
| Read-through | `bluetape4k-exposed` cache contract에 맞는 Exposed read-model repository. | raw JaVers `CdoSnapshot` repository 대체. |
| Write-through | 동기 Redis + database latency를 감수할 수 있는 mutable read model. | commit ordering을 보존해야 하는 JaVers audit write. |
| Write-behind | replay 또는 drain 실패 처리가 명시된 non-authoritative projection. | audit log write, commit sequence, repository head metadata. |
| Near-cache | Redisson 또는 Lettuce local cache를 활용하는 hot read-model lookup. | composite repository가 invalidation을 소유하지 않는 canonical snapshot/head state. |

`javers-persistence-redis`는 직접 Redis에 audit snapshot을 저장하는 모듈입니다.
이 SQL-backed repository의 write-behind cache로 감싸는 용도로 사용하지 마세요.
향후 composite repository는 durable history와 event/projection 동작을 결합할 수
있지만, invalidation, replay, failure semantics를 명시적으로 소유해야 합니다.

## 빌드

```bash
./gradlew :javers-exposed:test
```

## 참고 자료

- [JaVers](https://javers.org)
- [JetBrains Exposed](https://www.jetbrains.com/exposed/)
