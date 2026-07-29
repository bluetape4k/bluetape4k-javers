# Issue #138 Exposed EntityHook Audit 검토

## 범위

- Issue: #138, `feat: add Exposed DAO EntityHook audit adapter for JaVers`
- Branch: `feat/issue-138-exposed-entityhook-audit`
- Base: `develop` at `5266171`
- 변경 영역:
  - `javers-exposed` DAO hook public API
  - `javers-exposed` H2 hook integration tests
  - `javers-exposed` README locale set
  - Gradle `exposed-dao` catalog/dependency
  - spec, plan, and lesson artifacts

## 7-Tier 검토 결과

| Tier | 영역 | P0 | P1 | 비고 |
|---|---:|---:|---:|---|
| 1 | 정확성 | 0 | 0 | created/updated event는 flushed DAO state에 매핑된다. removed event는 JaVers terminal delete by id를 사용한다. |
| 2 | API / 호환성 | 0 | 0 | 새 API는 additive이며 `persistence.exposed.hook` 아래로 scope가 제한된다. 기존 repository API는 변경하지 않았다. |
| 3 | 영속성 / 트랜잭션 | 0 | 0 | rollback test는 source write와 JaVers row가 effective Exposed transaction을 공유함을 증명한다. |
| 4 | Lifecycle / Reentrancy | 0 | 0 | Subscription은 명시적 `close()`와 transaction-local reentrancy guard를 가진다. 테스트는 unsubscribe와 nested callback skip을 커버한다. |
| 5 | 테스트 | 0 | 0 | H2 tests는 create, update, delete, repeated update final state, rollback, close, reentrancy를 커버한다. 기존 DB matrix도 green 상태를 유지했다. |
| 6 | 문서 | 0 | 0 | English/Korean README는 DAO-only scope, lifecycle close, mapper contract, delete terminal snapshot behavior를 문서화한다. |
| 7 | Workflow / 증거 | 0 | 0 | issue를 먼저 갱신했고 spec/plan, lesson, 이 review를 PR 생성 전에 기록했다. |

## 증거

- GNO preflight:
  - `gno query "bluetape4k-javers 0.3.0 remaining issue javers-exposed entity hook flush audit" -c bluetape4k-github --fast --no-rerank`
  - `gno query "bluetape4k-javers 0.3.0 remaining issue javers-exposed entity hook flush audit" -c bluetape4k-docs --fast --no-rerank`
- Issue 갱신:
  - #138 본문을 현재 `develop=5266171` 및 #116/#115/#106/#103 이후 상태에 맞게 갱신했다.
- Source API checks:
  - Exposed 1.3.0 source jar에서 `EntityHook`, `registeredChanges()`, `EntityLifecycleInterceptor`, `transactionScope`를 확인했다.
  - JaVers 7.11.0 source jar에서 `commit()`과 `commitShallowDeleteById()`를 확인했다.
- Compile:
  - `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
- Focused hook tests:
  - `./gradlew :javers-exposed:test --tests 'io.bluetape4k.javers.persistence.exposed.hook.ExposedJaversEntityHookSubscriptionH2Test' --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - 7 tests executed.
- Targeted regression:
  - `./gradlew :javers-exposed:test :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` PASS.
  - latest run은 `javers-exposed` 53 tests를 실행했다. `javers-ddd`는 이 branch의 prior successful 11-test run 이후 up-to-date였다.
- Diff hygiene:
  - `git diff --check` PASS.
- IntelliJ diagnostics:
  - 이 세션에서 IntelliJ MCP diagnostics tool을 사용할 수 없어 Gradle compile과 targeted tests를 fallback evidence로 사용했다.

## Gate 판정

- P0=0
- P1=0
- Gate: PASS

## 잔여 위험

- Exposed `EntityHook`는 intermediate flush 이후 alert할 수 있으므로 adapter는 현재 flush/transaction state에 이미 등록된 event만 coalesce한다. future flush 전체에 걸친 full transaction-end buffering을 약속하지 않는다.
- Raw Exposed DSL write와 external database write는 설계상 scope 밖에 남는다.
