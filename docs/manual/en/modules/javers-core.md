# javers-core

`javers-core` is the shared Kotlin layer for JaVers: query extensions, codecs, change helpers, and `CdoSnapshotRepository` implementations backed by Caffeine, Cache2k, or JCache. Choose it alone for object diffing or process-local audit tests; add a persistence module when history must survive restarts.

## Dependency and core API

```kotlin
dependencies {
    implementation("io.github.bluetape4k.javers:javers-core")
}
```

`CdoSnapshotRepository` extends JaVers `JaversRepository` with single-snapshot save and newest-first GlobalId lookup. `AbstractCdoSnapshotRepository` supplies JaVers query filtering, codec integration, commit-head handling, and the persist loop used by every adapter. See [`CdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/CdoSnapshotRepository.kt) and [`AbstractCdoSnapshotRepository.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/AbstractCdoSnapshotRepository.kt).

## Runnable quick start

```kotlin
import io.bluetape4k.javers.repository.caffeine.CaffeineCdoSnapshotRepository
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id

data class Order(@Id val id: Long, var status: String)

val repository = CaffeineCdoSnapshotRepository()
val javers = JaversBuilder.javers()
    .registerJaversRepository(repository)
    .registerEntity(Order::class.java)
    .build()

val order = Order(1, "PLACED")
javers.commit("order-service", order)
order.status = "PAID"
javers.commit("order-service", order)

val changes = javers.findChanges(queryByInstanceId<Order>(1L))
check(changes.isNotEmpty())
```

The query helper is defined in [`QueryBuilderExtensions.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/main/kotlin/io/bluetape4k/javers/repository/jql/QueryBuilderExtensions.kt), and the same commit/query shape is exercised in [`CommitAndQueryExamples.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-core/src/test/kotlin/io/bluetape4k/javers/examples/CommitAndQueryExamples.kt).

## Persistence semantics and failure modes

Cache repositories hold encoded snapshots and commit sequences in the process. Eviction or restart loses history and can make JaVers see an object as new. They are suitable for tests, demos, bounded caches, or deliberately disposable history—not a durable audit ledger.

`AbstractCdoSnapshotRepository.persist` locks one repository instance, saves commit snapshots one at a time, then updates the head and its sequence. A failure stops the remaining steps but does not roll back an external write already completed. Broad JQL paths call `getAll()`, materialize every snapshot, and warn only when the key count exceeds 10,000. Keep production queries narrow and measure heap use.

Decoders may return `null`; cache and persistence adapters use `mapNotNull`, so corrupt payloads can disappear from results instead of failing the whole query. Monitor codec errors and test restoration with the exact codec used in production.

## Operations and testing

Treat repository lifetime, cache maximum size, eviction, codec version, and commit-ID sequence as operational configuration. A cache-backed repository recreated during a rolling restart begins empty.

```bash
./gradlew :javers-core:test
```

The release test suite covers commits, snapshots, shadows, codecs, and all three cache implementations. Add an application test that commits two versions and asserts `findChanges`, snapshot order, and shadow reconstruction.

## Non-goals

- It does not persist domain objects.
- Cache repositories do not provide durable history.
- It does not combine multiple snapshot repositories in release 0.3.0.
- Query helpers do not turn in-memory filtering into datastore pushdown.

Related reading: [audit model](../architecture/audit-model.md), [testing](../guides/testing.md), and [persistence selection](../persistence/selection-guide.md).
