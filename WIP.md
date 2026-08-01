# WIP - bluetape4k-javers

Snapshot: 2026-08-01 KST
범위: `debop`에게 할당된 열린 GitHub issue.
열린 issue 수: 1개.

## 현재 방향

`0.2.1`은 2026-06-27에 published된 최신 stable Javers release이며,
`bluetape4k-dependencies` `1.3.1` release가 소비하는 버전이다.

`0.2.1` patch lane에는 열린 GitHub issue가 없고 release/tag가 완료됐다.
다음 stable Javers release는 `0.3.0` development line의 작업과 별도로
정한다.

`0.3.0` line에서는 development가 열려 있으며, workflow-injected snapshot
publication을 위해 `snapshotVersion=`은 비워 둔다. train이 명시적으로 retarget되지
않는 한 `0.3.0`은 다음 dependencies release train의 stable Javers input이 아니다.
현재 `0.3.0`에서 `debop`에게 할당된 열린 issue는 없다. #289 한국어 `Fixed`
terminology follow-up은 PR #290으로 완료됐다.

## Milestone 현황

| Milestone | Open | Closed | 비고 |
|---|---:|---:|---|
| `0.2.1` | 0 | 29 | `bluetape4k-dependencies` `1.3.1`이 소비하는 published patch release. |
| `0.3.0` | 0 | 133 | #289 한국어 changelog terminology follow-up이 PR #290으로 완료됐다. |
| `backlog` | 1 | 6 | 향후 R2DBC persistence research이며 `0.3.0` blocker가 아니다. |

열린 PR: 없음.

## 활성 작업 대기열

| 우선순위 | Issue | Milestone | 비고 |
|---|---|---|---|
| P3 | [#119](https://github.com/bluetape4k/bluetape4k-javers/issues/119) research: evaluate R2DBC persistence support for JaVers snapshots | backlog | `0.3.0`과 분리해 유지하며, 구현 전에 backend feasibility research가 필요하다. |

## 최근 완료

- #77은 PR #85로 merge됐다: README persistence option diagram.
- #3은 PR #86으로 merge됐다: `javers-exposed` Exposed JDBC CDO snapshot repository.
- #4는 PR #87로 merge됐다: `javers-ddd` aggregate/domain-event helper.
- #88은 PR #91로 merge됐다: command-side `examples/javers-exposed-ddd` scaffold.
- #89는 PR #92로 merge됐다: Kafka to Redis projection flow.
- #90은 PR #93으로 merge됐다: Envers comparison benchmark result.
- #5 parent는 #88, #89, #90이 반영된 뒤 final tracking PR로 닫혔다.
- #95는 PR #96으로 merge됐다: `javers-exposed` database smoke coverage가 공유
  `bluetape4k-exposed-jdbc-tests` H2/PostgreSQL/MySQL_V8 matrix를 사용한다.
- PR #128이 `0.3.0` development line을 열었다.
- PR #206이 `compileTestKotlin` warning noise를 제거했고, #192, #193, #194,
  #208, #209, #210, #211, #212, #213, #224, #232, #233, #234가
  `0.3.0` line에서 닫혔다.
- #118 benchmark module move가 닫혔고, #195는 PR #219로 merge됐다:
  benchmark module README와 CI/Nightly smoke coverage.
- Epic #254와 #255-#271이 닫혔고, PR #272-#288이 한국어 문서/KDoc localization
  train과 final parity audit를 완료했다.
- PR #253이 GitHub Actions `actions/setup-python` dependabot update로 merge됐다.
- #289 한국어 `Fixed` terminology follow-up이 PR #290으로 merge됐다.

## 의존성 지도

```text
#5 examples/javers-exposed-ddd parent (complete)
  -> #88 command-side Exposed + JaVers + DDD helper flow
      -> #89 Kafka event consumer + Redis projection
          -> #90 Envers comparison benchmark results

#95 javers-exposed DB matrix (complete)
  -> bluetape4k-exposed-jdbc + bluetape4k-exposed-jdbc-tests
  -> H2 + PostgreSQL + MySQL_V8 default shared dialect set

#118 benchmark module move
  -> #195 benchmark README and smoke coverage

#192 release-prep review
  -> #208 DDD aggregate save audit/event consistency
  -> #209 durable snapshot commit atomicity
  -> #210 Lettuce repository lifecycle
  -> #211 Kafka projection replay head/sequence semantics
  -> #212 BOM publishable-module constraints
  -> #213 POM license metadata

#254 Korean localization epic (complete)
  -> #255-#271 scope, document, KDoc, and final parity work
      -> #289 Korean `Fixed` terminology follow-up (complete, PR #290)
```

## WIP 제한

| 작업선 | 제한 | 다음 작업 |
|---|---:|---|
| Data integrity blockers | 1 | 현재 WIP snapshot 기준 완료. |
| Release metadata blockers | 1 | 현재 WIP snapshot 기준 완료. |
| Documentation maintenance | 1 | 현재 WIP snapshot 기준 완료. |
| Future research | 1 | `0.3.0`이 정리됐으므로 #119 진행 가능 |

## 검증 증거

- 2026-08-01 KST에 PR #290 merge 후 live GitHub issue를 확인했다:
  `debop`에게 할당된 열린 issue는 #119 (`backlog`) 1개이며 #289는 닫혔다.
- 같은 시각 milestone을 확인했다: `0.2.1`은 0 open / 29 closed,
  `0.3.0`은 0 open / 133 closed, `backlog`는 1 open / 6 closed다.
- 2026-08-01 KST에 live GitHub release를 확인했다: `0.2.1`이 최신
  `bluetape4k-javers` release이며, `bluetape4k-dependencies` `1.3.1` catalog가
  `bluetape4k-javers-bom` `0.2.1`을 가리킨다.
- 2026-08-01 KST에 live GitHub PR을 확인했다: 열린 PR 없음.
- PR #290 merge 후 main worktree를 확인했다: `develop`은 clean이고
  `origin/develop`과 정렬돼 있었다.
