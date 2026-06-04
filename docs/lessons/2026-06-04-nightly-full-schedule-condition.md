# Lessons Learned — Nightly full schedule condition (2026-06-04)

**Related issue**: #152

## Context

The prior Nightly snapshot-refresh PR was marked merged, but current `develop`
did not contain the workflow updates. The follow-up also found that full-scope
scheduled jobs still compared `github.event.schedule` against the old Sunday
cron string.

## Decision

Re-apply the Nightly snapshot refresh/stagger changes on current `develop`, and
align full-scope job conditions with the repository's current Sunday schedule.

## Verification

- `actionlint .github/workflows/nightly-tests.yml`
- `git diff --check`
- Schedule-condition audit: no old `0 19 * * 0` full-job condition remains.

## Future Rule

When changing a scheduled cron string, update every `github.event.schedule`
comparison in the same workflow. If a PR is marked merged but `develop` does not
move, verify branch refs before deleting local recovery context.
