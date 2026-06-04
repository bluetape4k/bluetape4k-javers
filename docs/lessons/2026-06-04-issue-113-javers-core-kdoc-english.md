# Issue 113 javers-core KDoc English Cleanup

## Context

Issue #113 found remaining Korean KDoc and comments in production sources under
`javers-core/src/main/kotlin`.

## Decision

Translate public-facing production KDoc and comments to English without changing
behavior, signatures, README files, or API structure.

## Outcome

The remaining Korean text in `javers-core/src/main/kotlin` was removed from
base, codec, commit, diff, dispatcher, repository, and JQL extension APIs.

## Verification Evidence

- Korean text scan over `javers-core/src/main/kotlin`
- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --console=plain`
- `git diff --check`

## Future Guidance

Public production KDoc must stay English. Internal docs and lessons can be
Korean, but source KDoc should be checked before closing documentation cleanup
issues.
