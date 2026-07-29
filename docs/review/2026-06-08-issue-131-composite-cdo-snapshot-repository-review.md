# Issue 131 Composite CDO Snapshot Repository Review

## 범위

- Branch: `feat/issue-131-composite-repository`
- Base: `origin/develop@e54336d`
- Issue: `#131`
- 검토한 production scope:
  - `javers-core/src/main/kotlin/io/bluetape4k/javers/repository/composite/*.kt`
- 검토한 test scope:
  - `javers-core/src/test/kotlin/io/bluetape4k/javers/repository/composite/CompositeCdoSnapshotRepositoryTest.kt`
- 검토한 docs and assets:
  - `README.md`, `README.ko.md`
  - `javers-core/README.md`, `javers-core/README.ko.md`
  - `javers-core/docs/images/readme-diagrams/javers-core-composite-repository-01.{dot,plain,svg,png}`
  - issue spec and plan docs under `docs/superpowers/`

Native subagent note: Step 6-R은 보통 bounded native reviewer lane을 선호한다. 이 세션에서 노출된 `spawn_agent` schema에는 OMX-required `agent_type` field가 없었으므로, gate는 대신 local-equivalent 7-Tier review와 CodeGraph context를 사용했다.

## 증거

- Required references loaded:
  - `/Users/debop/.codex/skills/bluetape4k-full-feature/references/step-6r-code-review.md`
  - `/Users/debop/.codex/skills/bluetape4k-full-feature/references/step-4p-perf-scan.md`
  - `/Users/debop/.codex/skills/bluetape4k-code-patterns/SKILL.md`
  - `/Users/debop/.codex/skills/bluetape4k-diagram/SKILL.md`
- CodeGraph review context:
  - `status=ok`
  - `changed_files=11`
  - `impacted_files=0`
  - guidance: changes are well-contained with minimal blast radius.
- Static scans:
  - Kotlin forbidden-pattern scan은 새 composite source와 tests에서 match를 반환하지 않았다.
  - Legacy composite API-name 및 stray-text scan은 source, docs, README files에서 stale public name 또는 stray text를 찾지 못했다.
  - README image link는 `.png`를 가리키며, matching `.dot`, `.plain`, `.svg`, `.png`가 존재한다.
  - `rg "Inter|Arial|Helvetica" javers-core/docs/images/readme-diagrams/javers-core-composite-repository-01.svg` returned no matches.
- Visual evidence:
  - Rendered PNG를 `view_image`로 inspected했다.
  - Final README diagram은 original wide component layout을 유지하면서 title/subtitle을 포함한 기존 bluetape4k rounded outer-frame format을 적용한다.
  - Final diagram label은 English이며 visible하다. `1700x760` rendering 후 node/text overlap 또는 connector crowding은 관찰되지 않았다.
- Tests:
  - `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`
  - Result: `BUILD SUCCESSFUL`, `SUCCESS: Executed 197 tests in 13.8s`.
  - Earlier adapter regression: `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - Result: `BUILD SUCCESS`, `SUCCESS: Executed 39 tests in 12.8s`.
- IntelliJ diagnostics:
  - Exposed tool surface에서 IntelliJ MCP diagnostics tool을 사용할 수 없었다.
  - Fallback used: targeted Gradle compile/test, static scans, CodeGraph context, source/doc line review.

## 7-Tier 검토 결과

| Tier | 영역 | P0 | P1 | P2 | P3 | 리뷰 결과 |
|---|---:|---:|---:|---:|---:|---|
| 1 | Security | 0 | 0 | 0 | 0 | secret, unsafe deserialization, auth boundary, user-controlled query surface를 추가하지 않았다. Failure message는 snapshot identifier와 payload content를 피한다. |
| 2 | Ops/SRE reliability | 0 | 0 | 0 | 0 | Primary-first non-atomic behavior가 명시적이다. Delegate failure는 role, index, type, operation, cause를 보존한다. `close()`는 기본적으로 모든 closeable delegate에 close를 시도한다. |
| 3 | Structural impact | 0 | 0 | 0 | 0 | 새 package는 `javers-core` 아래 additive다. public read는 primary에 delegate한다. CodeGraph는 impacted existing node가 없다고 보고한다. |
| 4 | Kotlin code quality | 0 | 0 | 0 | 0 | validated data class에는 companion factory와 bluetape4k validation helper를 사용한다. English KDoc, forbidden Kotlin pattern 없음, coroutine/blocking surface 없음. |
| 5 | Tests/types/silent failure | 0 | 0 | 0 | 0 | 테스트는 constructor validation, read delegation, converter propagation, schema/write order, primary failure, fail-fast, best-effort, `persist(commit)`, JaVers commit 이후 primary reads, close aggregation, exception defensive copy를 커버한다. |
| 6 | Performance/stability | 0 | 0 | 0 | 0 | blocking/suspend path, retry loop, buffer, container, hot serialization을 추가하지 않았다. Fanout은 delegate count에 선형이며 의도적으로 synchronous하다. |
| 7 | Docs/release/evidence | 0 | 0 | 0 | 0 | Root 및 module README locale set를 갱신했다. diagram PNG/SVG pair가 존재한다. spec/plan은 실제 API name과 primary-first non-atomic semantics에 맞게 갱신됐다. |

## 리뷰 중 수정한 항목

| Priority | File:Line | 영역 | 결과 | 해결 |
|---|---|---|---|---|
| P2 | `CompositeCdoSnapshotException.kt:14` | API robustness | Public aggregate exception이 caller-provided list를 직접 받고 message construction 이후 validate했다. | Validated payload construction, immutable copy, `serialVersionUID`, regression test를 추가했다. |

## PR Comment 후속 조치

| Thread | 영역 | 해결 |
|---|---|---|
| `PRRT_kwDOSVj8-s6Hz43L` | README diagram | DOT source에 title/subtitle을 추가하고 Graphviz spacing을 늘린 뒤 `.plain`, `.svg`, `.png`를 다시 render하고 PNG를 inspected했다. |
| `PRRT_kwDOSVj8-s6Hz6GB` | Delegate kind model | delegate category API를 `CompositeCdoSnapshotDelegateKind`로 rename했다. primary는 정확히 하나이고 secondary는 0개 이상 ordered이므로 `PRIMARY`/`SECONDARY`를 유지했다. `delegateIndex`가 secondary execution order를 나타낸다는 점을 KDoc에 명확히 했다. |
| Follow-up | README diagram format | original wide component layout은 유지하고 final decoration만 기존 README infographic format으로 교체했다. rounded outer frame, title/subtitle header, semantic connector colors, hand-authored cards를 적용했고 PNG를 다시 render한 뒤 inspected했다. |

## 수렴 기록

| Iteration | P0 | P1 | P2 | P3 | Action |
|---|---:|---:|---:|---:|---|
| Initial local Step 6-R | 0 | 0 | 1 | 0 | public exception defensive-copy/validation robustness를 수정했다. |
| Post-fix Step 6-R | 0 | 0 | 0 | 0 | `javers-core:test`를 재실행했고 static scan은 계속 clean이다. |

## Gate 판정

`P0 = 0`, `P1 = 0`

Step 6-R gate: `PASS`.
