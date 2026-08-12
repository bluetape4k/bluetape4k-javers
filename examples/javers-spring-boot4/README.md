# examples-javers-spring-boot4

English | [한국어](./README.ko.md)

Spring Boot 4 REST example for JaVers auditing with Exposed JDBC command
persistence.

## Architecture

This example uses explicit Spring beans instead of auto-configuration.
`JaversExampleConfiguration` creates the H2-backed Exposed `Database`, initializes
the command-side and JaVers tables, registers `ExposedCdoSnapshotRepository`,
and wires the order repository, command handler, and REST controller.

![examples-javers-spring-boot4 wiring](../../docs/images/readme-diagrams/examples-javers-spring-boot4-wiring-01.png)

Runtime requests enter through `OrderController`. Write endpoints transition the
aggregate through `OrderCommandHandler`; the repository stores the source-of-
truth order row and then commits a JaVers snapshot with domain-event metadata.
Current reads use the command table, while history reads use JaVers snapshots
with the requested `limit` pushed into the JaVers query.

![examples-javers-spring-boot4 request audit flow](../../docs/images/readme-diagrams/examples-javers-spring-boot4-request-audit-flow-01.png)

## What This Example Covers

- Explicit Spring Boot 4 wiring for `Database`, `Javers`, and `OrderRepository`.
- Exposed-backed source-of-truth order persistence.
- JaVers snapshots persisted through `ExposedCdoSnapshotRepository`.
- `javers-ddd` aggregate repository and domain-event commit metadata.
- REST endpoints for order creation, payment, lookup, and audit history.
- H2-backed Spring MVC integration tests with `MockMvc`.
- PostgreSQL-backed Spring MVC verification through `bluetape4k-testcontainers`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Places an order and commits the first JaVers snapshot. |
| `POST` | `/orders/{orderId}/paid` | Marks an order as paid and commits a second snapshot. |
| `GET` | `/orders/{orderId}` | Returns current command-side order state. |
| `GET` | `/orders/{orderId}/history?limit=20` | Returns newest-first JaVers snapshot metadata after pushing the limit into the query. |

## Scope

This example uses current repository features only. It does not provide Spring
Boot auto-configuration, Redis projection endpoints, Kafka publication, or a
production outbox.

The Gradle project is named `:examples-javers-spring-boot4` so future publishing
rules can exclude example projects by prefix.

## Run

```bash
./gradlew :examples-javers-spring-boot4:test
```

The explicit database bean keeps H2 as the default and accepts
`javers.example.database.url`, `.driver`, `.username`, and `.password` overrides.
The integration suite uses `PostgreSQLServer.Launcher.postgres` to verify the same
bounded-history contract against PostgreSQL.
