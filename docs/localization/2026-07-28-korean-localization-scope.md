# 한국어 문서화 범위 잠금

이 문서는 GitHub Epic #254와 서브이슈 #255-#271의 실행 범위를 고정한다. 이번 작업의 목적은 `bluetape4k-javers` 저장소에서 README와 운영 지침을 제외한 단일 언어 문서, Kotlin 주석, KDoc을 한국어로 재작성하는 것이다.

## 적용 원칙

- GitHub issue와 PR 제목, 본문, 공개 메타데이터는 저장소 규칙에 따라 영어로 유지한다.
- `README.md`, `README.ko.md`, module `README.md` 계열은 이번 primary rewrite 범위에서 제외한다.
- `AGENTS.md`, `CLAUDE.md` 같은 LLM-facing 운영 지침은 영어 유지 대상이므로 제외한다.
- `docs/manual/en/**`와 `docs/manual/ko/**`는 bilingual pair로 관리되므로 primary rewrite 범위에서 제외하고 parity 검증 대상으로만 둔다.
- 코드 식별자, API 이름, Gradle task, shell command, URL, 정확한 에러 메시지는 번역하지 않는다.
- Kotlin 주석과 KDoc은 한국어 우선으로 재작성한다. `@property`, `@param`, `@return`, `@throws` 설명은 의미와 제약을 더 자세히 드러내도록 쓴다.
- production behavior 변경, API 변경, 테스트 로직 변경, 의존성 변경은 이번 Epic 범위가 아니다.

## 2026-07-28 기준 범위 수치

| 구분 | 수량 | 처리 |
|---|---:|---|
| README 계열 Markdown | 25 | 제외 |
| 운영 지침 문서 | 2 | 제외 |
| manual bilingual pair | 48 | parity 검증만 수행 |
| 단일 언어 후보 문서 | 159 | 한국어 재작성 |
| 주석/KDoc 포함 Kotlin 파일 | 107 | 한국어 재작성 |
| KDoc `@param`/`@property`/`@return`/`@throws` marker | 59 | 상세 한국어 설명으로 재작성 |

manual parity 기준은 `docs/manual/en/**` 24개, `docs/manual/ko/**` 24개, `en_only=0`, `ko_only=0`이다.

## 단일 언어 문서 묶음

| 묶음 | 파일 수 | 담당 이슈 |
|---|---:|---|
| `CHANGELOG.md` | 1 | #256 |
| `WIP.md` | 1 | #256 |
| `docs/governance/**` | 1 | #256 |
| `docs/lessons/**` | 86 | #257-#261 |
| `docs/review/**` | 31 | #262-#264 |
| `docs/research/**` | 2 | #265 |
| `docs/superpowers/**` | 37 | #265 |

`docs/lessons/**`는 날짜 범위로 나누어 PR 폭을 줄인다. `docs/review/**`는 초기 review 문서, snapshot/persistence review 문서, 나머지 review 문서로 나누어 검토한다.

## Kotlin 주석/KDoc 묶음

| 묶음 | 주석 포함 파일 수 | 담당 이슈 |
|---|---:|---|
| `javers-core` | 47 | #266, #267 |
| `javers-ddd` | 7 | #268 |
| `javers-exposed` | 4 | #268 |
| `javers-persistence-redis` | 4 | #269 |
| `javers-persistence-kafka` | 8 | #269 |
| `examples/javers-exposed-ddd` | 13 | #270 |
| `examples/javers-ktor` | 8 | #270 |
| `examples/javers-spring-boot4` | 10 | #270 |
| `javers-spring-boot4-autoconfigure` | 3 | #270 |
| `benchmark/javers-exposed-benchmark` | 2 | #270 |
| `buildSrc` | 1 | #270 |

Redis, Kafka, Testcontainers 기반 검증은 저장소 규칙에 따라 순차 실행한다.

## Stacked PR Train

| Train | 이슈 | 목적 |
|---|---|---|
| A | #255 -> #256 | 범위 잠금, 기본 문서 |
| B | #257 -> #261 | lesson 문서 날짜별 한국어화 |
| C | #262 -> #265 | review, research, superpowers 문서 한국어화 |
| D | #266 -> #270 | Kotlin 주석/KDoc module별 한국어화 |
| E | #271 | 최종 parity, 잔여 영어 scan, DoD 보고 |

각 PR은 직전 활성 localization branch를 base로 삼아 stack을 구성한다. Train 사이에서 충돌이 크거나 CI evidence가 약하면 해당 train을 더 작은 PR로 분할한다.

## 검증 명령

범위 잠금과 최종 audit에서 사용하는 기본 검증은 다음과 같다.

```bash
rg --files -g '*.md' -g '*.mdx'
rg --files -g 'docs/manual/**' -g '*.md' -g '*.mdx'
rg -n '@(param|property|return|throws)' --glob '*.kt' --glob '!**/build/**'
rg -l '(^\s*//|/\*\*|/\*|\*\s*@(param|property|return|throws))' --glob '*.kt' --glob '!**/build/**'
git diff --check
```

Kotlin 주석/KDoc PR은 변경 module에 맞는 Gradle task를 추가로 실행한다. Redis/Kafka persistence module은 동시에 실행하지 않는다.

## 완료 조건

- #255-#271이 모두 닫혀 있다.
- Epic #254 본문이 각 서브이슈와 PR train 완료 상태를 반영한다.
- README, operating docs, manual bilingual pair 제외가 유지된다.
- manual EN/KO basename parity가 `en_only=0`, `ko_only=0`으로 유지된다.
- 단일 언어 문서와 Kotlin 주석/KDoc에서 허용되지 않은 영어 prose가 남지 않는다.
- 모든 PR body의 마지막 Markdown `##` heading은 `## DoD Status`이다.
- merge-ready 이후 fresh approval을 받고 merge, local sync, cleanup이 완료되어 있다.
