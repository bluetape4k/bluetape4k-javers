# JaVers cache pipeline research lesson

## Context

The cache/pipeline planning issues for JaVers needed a durable research note in
this repository, not in `bluetape4k-wiki`.

## Decision

Store the source-backed note under `docs/research/` and link it from the GitHub
issues. Require future implementation to reuse `bluetape4k-projects/cache/*` and
`bluetape4k-exposed/exposed-jdbc-{lettuce,redisson}` before adding JaVers-only
cache abstractions.

## Outcome

The research now records the existing Redis/Lettuce/Redisson, Exposed cache,
pipeline, and virtual-thread constraints for issues #131 and #133-#136.

## Verification

- `git diff --check`
- GitHub issues #131, #133, and #134 updated with reuse and virtual-thread
  constraints.

## Future Guidance

When planning JaVers cache work, inspect `exposed-jdbc-lettuce` and
`exposed-jdbc-redisson` first. Keep JDBC paths virtual-thread friendly and avoid
new provider-neutral cache contracts inside `bluetape4k-javers`.
