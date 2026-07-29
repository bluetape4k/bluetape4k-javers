# 한국어 현지화 최종 감사

## 범위

Issue #271은 Epic #254의 최종 감사 slice다. 목표는 README를 제외한 단일 언어 문서와 코드 주석/KDoc의 한국어 재작성 상태를 검증하고, stacked PR train의 마지막 PR에 검증 증거를 남기는 것이다.

## 제외

- `README.md`, `README.ko.md` 및 module README locale pair는 이번 primary rewrite 범위에서 제외했다.
- `AGENTS.md`, `CLAUDE.md`, `.codex/**`, workflow prompt, skill 문서처럼 LLM-facing operating surface는 repo 규칙에 따라 English 유지 대상으로 뒀다.
- `docs/manual/en/**` 및 `docs/manual/ko/**`는 bilingual pair이므로 primary rewrite 대상이 아니라 parity verification 대상으로만 다뤘다.
- 명령어, Gradle task, GitHub URL, branch/PR/issue identifier, class/function/property name, package path, exact error string, test selector, dependency alias, code block example은 계약적 anchor로 보존했다.

## Manual parity 검증

- `docs/manual/en`: 24 markdown files.
- `docs/manual/ko`: 24 markdown files.
- EN/KO basename diff: 없음.

검증 명령:

```bash
bash -lc 'comm -3 <(cd docs/manual/en && find . -type f -name "*.md" | sed "s#^./##" | sort) <(cd docs/manual/ko && find . -type f -name "*.md" | sed "s#^./##" | sort)'
bash -lc 'printf "en="; find docs/manual/en -type f -name "*.md" | wc -l | tr -d " "; printf "ko="; find docs/manual/ko -type f -name "*.md" | wc -l | tr -d " "'
```

## 잔여 anchor

단일 언어 문서 scan에서 남은 match는 다음 범주다.

- `API Error: 400 This organization has been disabled.` 같은 과거 exact error string.
- `OrderSummary`, `RedisOrderSummaryProjection`, `CdoSnapshotRepositoryCodecContractTest` 같은 code identifier와 test selector.
- `Bean Validation`, `Code Review Graph`, `Phase 2 integration`, `Request DTO` 같은 프로젝트 내 고정 기술 용어.
- `## Behavior / Contract`처럼 과거 표준명을 설명하면서 현재 표준 `## 동작/계약`으로 전환했음을 기록하는 historical anchor.

검증 명령:

```bash
rg -n '(This |The |These |Those |When |Where |How |Why |Summary|Contract|Background|Problem|Solution|Implementation|Validation|Review|TODO|FIXME|Final Gate|PR creation is allowed|Date:|Scope:|Review source|Review target|Step [0-9].*Review|Tier [0-9].*And)' docs --glob '*.md' -g '!docs/manual/en/**' -g '!docs/manual/ko/**' -g '!**/README.md'
```

## 코드 주석 감사

남은 focused comment scan match는 `String codec`, `Map codec`, `Codec 연동`, `Mongo 연동`처럼 identifier와 한국어 설명이 함께 있는 section marker다. `GIVEN`, `WHEN`, `THEN`, `Noise`, `not supported` 같은 순수 영어 주석 marker는 한국어로 치환했다.

검증 명령:

```bash
rg -n '^\s*//\s*(GIVEN|WHEN|THEN|AND|Noise|String codec|Binary codec|Map codec|Cache for|Mongo|Codec|Compression|Test|Javers|Kafka is write-only|KafkaTemplate requires|Build a KafkaTemplate|Redis projection target tests|primitives|collections|array|reference|not supported)' . --glob '*.kt' --glob '*.kts'
```

## Stacked PR train

- #272: scope and inventory.
- #273-#282: single-language document localization slices.
- #283-#287: KDoc/comment localization slices.
- #288: final parity and DoD audit.

각 PR은 English GitHub title/body 정책을 따르며, body의 마지막 H2 section은 `## DoD Status`여야 한다.

## 결론

Issue #271 기준으로 primary scope의 남은 영어 prose는 기술 anchor 또는 exact evidence로 분류된다. README, operating docs, bilingual manual pair는 의도적으로 primary rewrite scope에서 제외됐고, manual EN/KO file parity는 PASS다.
