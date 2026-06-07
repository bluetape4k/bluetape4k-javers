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
- repository instance를 다시 만들 때 head commit id를 복원합니다.
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

## Query 동작

첫 구현은 global id로 snapshot을 읽은 뒤 상위 repository의 JaVers query
filtering 동작에 위임합니다. SQL pushdown은 이번 module version의 범위가
아니며, 같은 repository API 뒤에서 이후 확장할 수 있습니다.

## 빌드

```bash
./gradlew :javers-exposed:test
```

## 참고 자료

- [JaVers](https://javers.org)
- [JetBrains Exposed](https://www.jetbrains.com/exposed/)
