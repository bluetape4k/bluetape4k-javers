# Getting started

The first setup decision is not which Javers module version matches which Exposed, Redis, or Kafka version. Choose one released `bluetape4k-dependencies` ecosystem version that contains Javers `0.3.0`, then declare only the modules the service uses. The ecosystem platform coordinates the repository BOMs and shared libraries.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<ecosystem-version>"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

Do not add independent Projects, Exposed, Redis, Kafka, and Javers versions around this block. If your organization cannot use the ecosystem platform, the narrower `bluetape4k-javers-bom:0.3.0` is a fallback, but then the application owns cross-repository compatibility.

## A durable first repository

```kotlin
val repository = ExposedCdoSnapshotRepository(database)
repository.ensureSchema()

val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

javers.commit("order-service", order)
```

`ensureSchema()` creates `javers_commit` and `javers_snapshot`. This convenience is useful for a local run; production schema changes remain the deployment owner's responsibility. Every repository operation opens an Exposed transaction, so an application-domain transaction and the JaVers write are not automatically one atomic unit. See the [Exposed guide](persistence/exposed.md) before treating the snippet as production wiring.

The class and table contracts are visible in [`ExposedCdoSnapshotRepository`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepository.kt) and [`JaversExposedTables.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/main/kotlin/io/bluetape4k/javers/persistence/exposed/schema/JaversExposedTables.kt).

Next, read [the audit model](architecture/audit-model.md), then choose a persistence role in [the selection guide](persistence/selection-guide.md).
