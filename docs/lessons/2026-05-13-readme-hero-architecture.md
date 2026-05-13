# README Hero And Architecture Refresh

## Context

The JaVers repository README was still a short note and did not show the Redis,
Kafka, BOM, or planned Exposed/DDD direction.

## Decision

Store the generated JaVers audit workbench image in `docs/assets/javers-workbench.png`
and refresh both README locales with purpose, features, architecture, and module
tables.

## Outcome

The root README now presents the audit/diff, Redis persistence, Kafka
persistence, BOM, and phase-chain backlog at the entrypoint.

## Verification

- Confirmed the generated asset exists as a PNG under `docs/assets`.
- Verified both README locales reference the shared image path.

## Future Guidance

When implementing `javers-exposed` or DDD helpers, update README architecture,
WIP, `AGENTS.md`, and `CLAUDE.md` together.
