# 교훈: KDoc을 한국어에서 영어로 번역 (이슈 #30)

**날짜**: 2026-05-17
**관련 이슈**: [#30](https://github.com/bluetape4k/bluetape4k-javers/issues/30)
**PR**: docs/kdoc-translate-to-english

## 변경 파일 (6개)

| 파일 | 번역 대상 |
|---|---|
| `javers-core/.../commit/SnowflakeCommitIdGenerator.kt` | 클래스 KDoc, `getSeq` KDoc, `@property` |
| `javers-core/.../metamodel/GlobalIdExtensions.kt` | `isParent`, `isChild` KDoc |
| `javers-core/.../metamodel/CdoSnapshotSupport.kt` | 모든 확장 함수 KDoc (11개) |
| `javers-core/.../QueryParamsExtensions.kt` | `isDateInRange` KDoc |
| `javers-core/.../CdoExtensions.kt` | `getWrappedOrNull` KDoc |
| `javers-persistence-redis/.../RedissonCdoSnapshotRepository.kt` | 클래스 KDoc, 속성 KDoc 2개, 인라인 주석 |

## 번역 기준 적용

- 섹션 제목: `## 동작/계약` → `## Behavior / Contract`
- `~한다` 서술형 → 3인칭 단수 (`Returns`, `Filters`, `Maps` 등)
- `true/false 반환` → `Returns true when ... / Returns false otherwise`
- 예제 주석 내 한국어 설명 영어화

## 향후 지침

- 새 공개 API를 추가할 때는 처음부터 영어로 KDoc을 작성한다(`CLAUDE.md` 정책).
- 계약을 명시하는 API에는 `## Behavior / Contract` 섹션을 표준으로 적용한다.
