# examples-javers-ktor

English | [한국어](./README.ko.md)

Ktor REST example for JaVers auditing with Exposed JDBC command persistence.

## Architecture

This example is intentionally explicit. The Ktor module creates the H2-backed
Exposed `Database`, creates the command-side and JaVers tables, registers
`ExposedCdoSnapshotRepository`, and then wires a small order API.

![examples-javers-ktor wiring](../../docs/images/readme-diagrams/examples-javers-ktor-wiring-01.png)

Runtime requests go through Ktor routes into the command handler. Order state is
stored in the `example_order` table first, then `AggregateRepository` commits a
JaVers snapshot with domain-event metadata. History reads come from JaVers
snapshots, while current order reads come from the command table.

![examples-javers-ktor request audit flow](../../docs/images/readme-diagrams/examples-javers-ktor-request-audit-flow-01.png)

## What This Example Covers

- Explicit Ktor wiring for `Database`, `Javers`, and `OrderRepository`.
- Exposed-backed source-of-truth order persistence.
- JaVers snapshots persisted through `ExposedCdoSnapshotRepository`.
- `javers-ddd` aggregate repository and domain-event commit metadata.
- `bluetape4k-ktor-core` JSON error, health, and readiness routes.
- Ktor endpoints for order creation, payment, lookup, and audit history.
- H2-backed Ktor `testApplication` integration tests.
- PostgreSQL-backed Ktor integration verification through `bluetape4k-testcontainers`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Places an order and commits the first JaVers snapshot. |
| `POST` | `/orders/{orderId}/paid` | Marks an order as paid and commits a second snapshot. |
| `GET` | `/orders/{orderId}` | Returns current command-side order state. |
| `GET` | `/orders/{orderId}/history?limit=20` | Returns newest-first JaVers snapshot metadata. |
| `GET` | `/healthz` | Returns the bluetape4k Ktor health response. |
| `GET` | `/readyz` | Returns the bluetape4k Ktor readiness response. |

## Scope

This example uses current repository features only. It does not provide Redis
projection endpoints, Kafka publication, a production outbox, or Spring Boot
auto-configuration.

The Gradle project is named `:examples-javers-ktor` so future publishing rules
can exclude example projects by prefix.

The example uses synchronous Exposed JDBC because the current JaVers Exposed
repository is JDBC-backed. Every request-side JDBC and JaVers call runs inside
the caller-provided `blockingDispatcher`, which defaults to `Dispatchers.IO`.
The optional `database` parameter is caller-owned; omitting it keeps the
H2-backed example default. For high-concurrency production Ktor deployments,
provide an application-owned JDBC database and an appropriately bounded
blocking dispatcher, or evaluate a virtual-thread runtime strategy or future
R2DBC path.

The test suite keeps fast H2 coverage and also runs a real PostgreSQL path with
`PostgreSQLServer.Launcher.postgres`. The latter proves schema creation, command
state persistence, and JaVers history against the JDBC driver and database
engine; it is not replaced by H2 compatibility mode.

## Run

```bash
./gradlew :examples-javers-ktor:test
```
