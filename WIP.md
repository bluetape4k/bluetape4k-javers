# WIP - bluetape4k-javers

스냅숏: 2026-09-02 KST
범위: 1.0.0 release-prep와 별도 backlog.
열린 issue 수: 2개 (#363, backlog #119).

## 현재 방향

`0.3.0`은 2026-08-06에 게시된 최신 안정 Javers 릴리스다. 현재 개발선은
`baseVersion=1.0.0`, 빈 `snapshotVersion`을 사용하며, 1.0.0 release-prep #363만
배포 blocker로 남아 있다. 중앙 catalog는 Projects/Exposed 안정 버전이 승격된
immutable SHA `8efed120b91c4e1b1cfbfe1269321df325b08aef`를 사용한다.

backlog #119의 R2DBC persistence research는 1.0.0 배포와 분리해 유지한다.

## Milestone 현황

| Milestone | Open | Closed | 비고 |
|---|---:|---:|---|
| `1.0.0` | 1 | 58 | #363 release-prep만 남아 있다. |
| `backlog` | 1 | 7 | 향후 R2DBC persistence research이며 `1.0.0` blocker가 아니다. |

열린 PR: 없음.

## 활성 작업 대기열

| 우선순위 | Issue | Milestone | 비고 |
|---|---|---|---|
| P1 | [#363](https://github.com/bluetape4k/bluetape4k-javers/issues/363) 1.0.0 정식 배포 경계 준비 | 1.0.0 | catalog SHA·문서·exact-head CI·Full Nightly·공개 아티팩트 검증 |
| P3 | [#119](https://github.com/bluetape4k/bluetape4k-javers/issues/119) JaVers snapshot의 R2DBC persistence 지원 평가 | backlog | `1.0.0`과 분리해 유지하며, 구현 전에 backend feasibility research가 필요하다. |

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
| 데이터 무결성 blocker | 1 | 현재 WIP 스냅숏 기준 완료. |
| 릴리스 메타데이터 blocker | 1 | 현재 WIP 스냅숏 기준 완료. |
| 문서 유지보수 | 1 | 현재 WIP 스냅숏 기준 완료. |
| 향후 연구 | 1 | `0.3.0`이 정리됐으므로 #119 진행 가능 |

## 검증 증거

- 2026-09-02 KST에 live GitHub state를 확인했다. `1.0.0` milestone은 #363 한 건만
  열려 있고 58건이 닫혔으며, `backlog`는 #119 한 건만 열려 있다.
- 같은 시각 최신 안정 GitHub Release는 `0.3.0`, 열린 PR은 0건임을 확인했다.
- 1.0.0 release-prep는 중앙 catalog exact SHA
  `8efed120b91c4e1b1cfbfe1269321df325b08aef`를 사용한다.
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
