# 2026-05-26 — Issue 77 README Persistence Diagram

## Context

Issue #77 asked for a README-facing relationship diagram covering Redis, Kafka,
and Exposed persistence options.

## Decision

Use the repo-local root README asset location `docs/assets/` and share one
English diagram image between `README.md` and `README.ko.md`. The diagram shows
current Redis/Kafka source-backed modules and marks `javers-exposed` as the
issue #3 implementation target because the module is not present yet.

## Outcome

Added SVG/PNG assets plus Graphviz layout evidence for the persistence options
diagram, then embedded the PNG in both root README files.

## Verification

- Rendered `docs/assets/javers-persistence-options.png` from the SVG.
- Inspected the rendered PNG visually.
- Ran `xmllint --noout` on the final SVG and Graphviz sketch SVG.
- Ran `git diff --check`.

## Future Guidance

After issue #3 lands, update the diagram wording from planned Exposed support to
implemented Exposed JDBC persistence and refresh the README module table if the
new artifact is published.
