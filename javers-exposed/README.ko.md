# Module bluetape4k-javers-exposed

[English](./README.md) | 한국어

[JaVers](https://javers.org) CDO snapshot을 위한 Exposed JDBC persistence 모듈입니다.
`ExposedCdoSnapshotRepository`는 JetBrains Exposed로 관리되는 SQL table에
JaVers commit과 encoded snapshot을 저장하는 `AbstractCdoSnapshotRepository`
구현체입니다.

## 기능

- JaVers commit마다 `javers_commit` row를 저장합니다.
- CDO snapshot version마다 `javers_snapshot` row를 저장합니다.
- JaVers JSON converter로 snapshot을 복원할 수 있도록 전체 encoded
  `CdoSnapshot` payload를 저장합니다.
- repository instance를 다시 만들 때 head commit id를 복원합니다.
- Exposed JDBC를 통해 H2, PostgreSQL, MySQL을 지원합니다.

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
