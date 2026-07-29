# Issue 153 코드 검토

## 범위

Issue #153을 위해 `.github/workflows/ci.yml`, `.github/workflows/nightly-tests.yml`,
`docs/governance/kover-coverage-policy.md`, and
`docs/lessons/2026-06-05-issue-153-kover-coverage-reports.md`를 review한다.

## 7-Tier 검토

| Tier | 초점 | P0 | P1 | P2 | P3 | 비고 |
|---|---|---:|---:|---:|---:|---|
| 1 | Correctness | 0 | 0 | 0 | 0 | CI test jobs는 이제 Kover XML report를 생성하고 정확한 `report.xml` artifact를 upload한다. |
| 2 | Reliability | 0 | 0 | 0 | 0 | Nightly는 더 이상 `continue-on-error`로 Kover generation failure를 숨기지 않는다. |
| 3 | Security | 0 | 0 | 0 | 0 | Workflow permission은 변경되지 않았고 read-only로 유지된다. |
| 4 | Maintainability | 0 | 0 | 0 | 0 | Artifact name은 CI, Nightly, full-scope expected list 전반에서 정렬된다. |
| 5 | Test Coverage | 0 | 0 | 0 | 0 | Local `:javers-core:koverXmlReport`가 report path와 task behavior를 증명한다. |
| 6 | CI/Operations | 0 | 0 | 0 | 0 | Sunday full-scope predicate는 이제 active `15 19 * * 0` cron과 일치한다. |
| 7 | Docs/Evidence | 0 | 0 | 0 | 0 | Governance 및 lesson docs는 mandatory report generation과 report-only threshold를 기록한다. |

## 검증

- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`: PASS
- `./gradlew :javers-core:koverXmlReport --no-configuration-cache --no-build-cache --no-daemon --console=plain`: PASS, 175 tests
- Workflow scan for stale `0 19 * * 0` predicates and broad Kover directory uploads: PASS
- Coverage artifact validation dry-runs: PASS
- `git diff --check`: PASS

## 판정

P0 = 0. P1 = 0. PR body verification 이후 PR은 CI로 진행할 수 있다.
