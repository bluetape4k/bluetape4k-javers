# JaVers 0.1.1 Release Prep

## Context

The 0.1.1 milestone has no open issues and the repository is still on the
post-0.1.0 snapshot line. The catalog still referenced the older
`bluetape4k-bom:1.8.0`.

## Decision

Prepare `bluetape4k-javers` 0.1.1 as a release and align it with
`bluetape4k-bom:1.9.0`.

## Outcome

Release metadata, CHANGELOG, WIP, and catalog versions were updated for 0.1.1.

## Verification

Pending release validation must include Gradle version checks, POM generation,
POM scans for stale/snapshot artifacts, actionlint, and CI before tagging.

## Future Notes

Do not fold unmilestoned correctness work such as #62 into a patch release
without explicitly moving it into the release milestone first.
