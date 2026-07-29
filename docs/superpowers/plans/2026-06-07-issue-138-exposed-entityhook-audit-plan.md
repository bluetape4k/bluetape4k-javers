# Issue #138 — Exposed EntityHook JaVers Audit Plan

## Lane

Full Feature / Type A: public API, 새 DAO dependency surface, transaction-bound
Exposed behavior, README locale updates, local review gate.

## 작업

1. Spec/plan gate
   - current dependency evidence로 issue body를 refresh한다.
   - local source jar에서 Exposed 및 JaVers source API를 확인한다.
   - 이 spec과 plan을 작성한다.

2. Dependency 및 API implementation
   - `javers-exposed`에 `exposed-dao` alias/dependency를 추가한다.
   - `ExposedJaversEntityHookMapping`을 추가한다.
   - `ExposedJaversEntityHookSubscription`을 추가한다.
   - bluetape4k validation helper와 English KDoc을 사용한다.

3. Tests
   - H2 DAO table/entity/test model을 추가한다.
   - create, update, delete, final-event-only coalescing, rollback, close/unsubscribe를 커버한다.
   - Testcontainers/DB verification은 serial로 유지한다.

4. README 및 lesson
   - `javers-exposed/README.md`를 갱신한다.
   - `javers-exposed/README.ko.md`를 갱신한다.
   - `docs/lessons/` 아래 concise lesson을 추가한다.

5. 검증
   - touched module을 compile한다.
   - targeted javers-exposed 및 javers-ddd tests를 serial로 실행한다.
   - `git diff --check`를 실행한다.

6. 검토 및 PR
   - tracked `docs/review/2026-06-07-issue-138-exposed-entityhook-audit-review.md`를 만든다.
   - Local review는 PR 전에 P0=0 및 P1=0을 보고해야 한다.
   - Lore trailer로 commit한다.
   - assignee `debop`, milestone `0.3.0`으로 #138을 해결하는 PR을 만든다.
   - live PR body와 final `## DoD Status` section을 검증한다.

## 중단 조건

implementation이 commit되고, PR이 생성되고, PR body가 verified되며, local review가
P0=0/P1=0을 가지면 중단한다. explicit user request 없이 merge하지 않는다.

## 알려진 위험

- `EntityHook`는 global이므로 누락된 `close()` call은 subscriber를 leak할 수 있다.
- Exposed DAO lifecycle은 DSL write를 커버하지 않는다. README가 과장하면 안 된다.
- JaVers terminal delete는 full deleted object state가 아니라 local id/type만 사용한다.
