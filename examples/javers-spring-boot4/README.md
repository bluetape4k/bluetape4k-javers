# examples-javers-spring-boot4

English | [한국어](./README.ko.md)

Spring Boot 4 REST example for JaVers auditing with Exposed JDBC command
persistence.

## What This Example Covers

- Explicit Spring Boot 4 wiring for `Database`, `Javers`, and `OrderRepository`.
- Exposed-backed source-of-truth order persistence.
- JaVers snapshots persisted through `ExposedCdoSnapshotRepository`.
- `javers-ddd` aggregate repository and domain-event commit metadata.
- REST endpoints for order creation, payment, lookup, and audit history.
- H2-backed Spring MVC integration tests with `MockMvc`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Places an order and commits the first JaVers snapshot. |
| `POST` | `/orders/{orderId}/paid` | Marks an order as paid and commits a second snapshot. |
| `GET` | `/orders/{orderId}` | Returns current command-side order state. |
| `GET` | `/orders/{orderId}/history?limit=20` | Returns newest-first JaVers snapshot metadata. |

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
