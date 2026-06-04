# Issue 111 Nightly Coverage Artifact Gate

## Context

Issue #111 found that nightly full coverage aggregation downloaded `coverage-*`
artifacts with `continue-on-error: true` and then uploaded whatever was present.
That made missing module coverage artifacts hard to notice.

## Decision

Keep the artifact download step tolerant so the aggregation job can print a
specific validation message, then fail the job when any full-scope coverage
artifact is missing or empty.

## Outcome

`nightly-tests.yml` now defines the expected full-scope coverage artifact names
in the coverage aggregation job and validates them before uploading
`coverage-all`.

## Verification Evidence

- `actionlint .github/workflows/nightly-tests.yml`
- Dry-run shell validation with all expected artifacts present
- Dry-run shell validation with one expected artifact missing
- `git diff --check`

## Future Guidance

When adding, renaming, or removing nightly modules, update the test job, coverage
artifact upload name, coverage aggregation `needs`, and expected artifact list in
one change.
