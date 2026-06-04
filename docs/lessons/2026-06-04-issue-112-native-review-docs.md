# Issue 112 Native Review Documentation Cleanup

## Context

Several historical `docs/superpowers/**` and `docs/lessons/**` files still used
old external CLI review language in ways that could read like current process
requirements.

## Decision

Preserve the historical fact that external review attempts happened, but remove
wording that makes those tools look like required gates. Current work must use
local/native 7-tier review with P0=0 and P1=0 as the required review evidence.

## Outcome

Historical design, plan, and lesson notes now describe external tool outages as
past context only. Active gate language points to local/native review and CI
evidence.

## Verification Evidence

- Review-gate keyword scan over `docs`
- `git diff --check`

## Future Guidance

When preserving old execution evidence, label unavailable external tool output
as historical context and keep the active DoD gate tied to local/native review.
