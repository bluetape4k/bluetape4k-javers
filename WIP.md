# WIP - bluetape4k-javers

Snapshot: 2026-06-26 KST
범위: `debop`에게 할당된 열린 GitHub issue.
열린 issue 수: 5개.

## 현재 방향

`0.2.0`은 최신 published stable Javers release이며
`bluetape4k-dependencies` `1.2.0`이 소비하는 버전이다.

`0.2.1` patch lane에는 열린 GitHub issue가 없지만 아직 tag 또는 publish가
완료되지 않았다. release가 완료되고 Maven Central visibility가 검증될 때까지
`0.2.1`을 다음 `bluetape4k-dependencies` release train의 stable Javers
candidate로 취급한다.

`0.3.0` line에서는 development가 열려 있으며, workflow-injected snapshot
publication을 위해 `snapshotVersion=`은 비워 둔다. train이 명시적으로 retarget되지
않는 한 `0.3.0`은 다음 dependencies release train의 stable Javers input이 아니다.

## Milestone 현황

| Milestone | Open | Closed | 비고 |
|---|---:|---:|---|
| `0.2.1` | 0 | 21 | 다음 dependencies train의 patch candidate이며 tag/release는 아직 없다. |
| `0.3.0` | 4 | 77 | 0.2.1 patch lane 이후의 다음 development queue. |
| `backlog` | 1 | 4 | 향후 R2DBC persistence research이며 `0.3.0` blocker가 아니다. |

열린 PR: 없음.

## 활성 작업 대기열

| 우선순위 | Issue | Milestone | 비고 |
|---|---|---|---|
| P1 | [#208](https://github.com/bluetape4k/bluetape4k-javers/issues/208) fix: make DDD aggregate save audit/event boundary atomic | 0.3.0 | #192에서 발견된 release-prep data-integrity blocker. |
| P1 | [#209](https://github.com/bluetape4k/bluetape4k-javers/issues/209) fix: make durable snapshot persist commit-atomic | 0.3.0 | #192에서 발견된 release-prep data-integrity blocker. |
| P1 | [#211](https://github.com/bluetape4k/bluetape4k-javers/issues/211) fix: preserve Kafka projection head and sequence semantics | 0.3.0 | #192에서 발견된 release-prep replay consistency blocker. |
| P2 | [#118](https://github.com/bluetape4k/bluetape4k-javers/issues/118) build: move Envers comparison benchmark into a benchmark module | 0.3.0 | Benchmark module registration 작업이며 Gradle projects/tasks와 benchmark docs를 검증한다. |
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
- PR #206이 `compileTestKotlin` warning noise를 제거했다.
- #192, #193, #194, #210, #212, #213은 `0.3.0` line에서 닫혔다.
- #195는 PR #219로 merge됐다: benchmark module README와 CI/Nightly smoke coverage.

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
```

## WIP 제한

| 작업선 | 제한 | 다음 작업 |
|---|---:|---|
| Data integrity blockers | 1 | #208, then #209, then #211 |
| Release metadata blockers | 1 | 현재 WIP snapshot 기준 완료. |
| Small refactor / maintenance | 1 | 현재 WIP snapshot 기준 완료. |
| Lifecycle cleanup | 1 | 현재 WIP snapshot 기준 완료. |
| Benchmark build/docs | 1 | #118 |
| Future research | 1 | `0.3.0`이 정리된 뒤 #119만 진행 |

## 검증 증거

- 2026-06-26 KST에 live GitHub issue를 확인했다:
  `0.2.1`에는 열린 issue가 0개, `0.3.0`에는 열린 issue가 4개, `backlog`에는
  열린 issue가 1개였다.
- 2026-06-26 KST에 live GitHub release를 확인했다: `0.2.0`은 존재하고
  `0.2.1`은 아직 없다.
- 2026-06-26 KST에 live GitHub PR을 확인했다: 열린 PR 없음.
- 2026-06-26 KST에 main worktree를 확인했다: `develop`은 clean이고
  `origin/develop`과 정렬돼 있었다.
