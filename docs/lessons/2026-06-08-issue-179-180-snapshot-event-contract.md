# Issue #179/#180 - Snapshot Event Contract Documentation

## Context

Post-merge review of PR #175 found two documentation risks in the Kafka snapshot
event pipeline:

- The in-process `CdoSnapshotEvent` metadata contract could be mistaken for a
  Kafka wire envelope or header contract.
- Multi-snapshot commits can partially publish earlier snapshot events before a
  later snapshot publish fails, so retry behavior is at-least-once and can
  duplicate events.

## Decision

Document the current Kafka behavior exactly:

- Kafka record values remain encoded JaVers snapshot payloads.
- Snapshot metadata is in-process adapter metadata unless a future adapter
  defines headers or an envelope explicitly.
- Kafka publishing is synchronous at-least-once.
- Current Kafka consumers should treat duplicates as possible because the
  idempotency key is not wire-visible today.
- Projection/replay work or future wire contracts should expose and use the
  opaque idempotency key, or define another transport-specific deduplication
  policy.

## Outcome

The README locale pair now separates in-process event metadata from Kafka wire
payloads and documents partial publish/retry behavior for #105 and #131.

## Future Guard

When adding NATS, SQS, projection, or composite repository work, do not imply
metadata is wire-visible unless the adapter defines and tests headers or an
envelope.
