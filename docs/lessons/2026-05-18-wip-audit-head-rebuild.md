# 2026-05-18 — JaVers WIP audit and Redis head rebuild risk

## Context

The WIP queue still listed closed 0.1.0 release-prep issues (#29 through #32)
after the release work had been completed. A GNO lookup pointed to the recent
0.1.0 pre-release fix lesson, then GitHub issue state was used as the source of
truth for the current open queue.

## Decision

Register #62 for the remaining Redis persistence correctness risk: persistent
Lettuce/Redisson repositories store snapshots and commit sequence metadata, but
`AbstractCdoSnapshotRepository.getHeadId()` returns only the in-memory `head`
field after repository rebuild.

## Outcome

`WIP.md` now lists four open issues assigned to `debop`: #62, #3, #4, and #5.
The release-prep chain is moved to recently completed work, and #62 is the next
correctness item before Redis-backed examples expand.

## Verification

- `gh issue list --state open --assignee debop` returned four open issues.
- `gh issue view 62` confirmed #62 is open, labelled `bug`, and assigned to
  `debop`.
- `rg` confirmed #62 and the open count are present in `WIP.md`.

## Future Agents

When auditing WIP files, prefer GNO for historical context, then reconcile with
live GitHub state before editing queue counts or priorities.
