# 2026-05-27 — issue 102 Kafka read-path warning noise

## Context

`KafkaCdoSnapshotRepository` is intentionally write-only. Its read-path methods returned empty/false/0 but logged a warning on every call, so normal JaVers read probes or misconfigured applications could flood logs.

## Decision

Keep the write-only read contract unchanged and log the contract warning only once per repository instance. Repeated read-path contract messages move to debug level.

## Outcome

The repository now uses an AtomicFU flag for once-per-instance warning behavior. The Kafka module test suite verifies the empty/false/0 read results, one WARN event, and repeated DEBUG events.

## Verification

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --console=plain`
  - Result: success, 5 tests executed.

## Future Guard

Do not widen protected read methods only for tests. If the read contract needs direct testing, keep reflection helpers in tests or add a purpose-built internal test fixture without changing public API.
