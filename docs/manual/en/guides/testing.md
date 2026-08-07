# Testing audit paths

An audit test should prove the boundary most likely to fail, not merely that a commit returns.

## Small tests

Use cache-backed repositories for fast commit, snapshot, change, and shadow tests. Verify author and commit properties, snapshot order, and reconstruction. Codec tests should include corrupted data because several decoders return `null` rather than throwing. If your code uses `ShadowProvider`, include a JaVers compatibility smoke test because it reflects into `typeMapper`.

## Persistence tests

For Exposed, test schema creation separately from migration-managed startup. Commit more than one version, reconstruct the repository, and verify head restoration and newest-first history. Exercise a duplicate GlobalId/version and a failure between snapshot and sequence updates. The release tests cover H2 plus database smoke paths in [`ExposedCdoSnapshotRepositoryH2Test.kt`](https://github.com/bluetape4k/bluetape4k-javers/blob/978d0490fc438570e7520643aed50e20614772d1/javers-exposed/src/test/kotlin/io/bluetape4k/javers/persistence/exposed/repository/ExposedCdoSnapshotRepositoryH2Test.kt).

For Redis, run Lettuce and Redisson suites independently against Testcontainers. Verify repository reconstruction, history, shadow loading, connection failure, and your chosen persistence/eviction settings. For Kafka, assert the produced key and payload, the 30-second failure boundary, and the fact that reads stay empty.

## End-to-end projection test

`OrderProjectionFlowTest` starts Kafka and Redis through bluetape4k Testcontainers, uses H2 for domain and audit tables, publishes `OrderPlaced` and `OrderMarkedPaid`, polls Kafka, and verifies Redis status. It proves the happy-path ordering under those fixtures. It does not prove crash recovery, duplicate delivery handling, offset correctness, or production database behavior.

Run the module test that owns the boundary; keep Redis and Kafka container suites sequential when shared launchers or ports can interact. The [Projects manual](https://bluetape4k.github.io/manual/bluetape4k-projects/) covers the underlying Testcontainers launchers.
