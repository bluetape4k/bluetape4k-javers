# Lesson: KDoc 한국어 → 영어 번역 (issue #30)

**날짜**: 2026-05-17
**관련 이슈**: [#30](https://github.com/bluetape4k/bluetape4k-javers/issues/30)
**PR**: docs/kdoc-translate-to-english

## 변경 파일 (6개)

| 파일 | 번역 대상 |
|---|---|
| `javers-core/.../commit/SnowflakeCommitIdGenerator.kt` | 클래스 KDoc, `getSeq` KDoc, `@property` |
| `javers-core/.../metamodel/GlobalIdExtensions.kt` | `isParent`, `isChild` KDoc |
| `javers-core/.../metamodel/CdoSnapshotSupport.kt` | 모든 extension function KDoc (11개) |
| `javers-core/.../QueryParamsExtensions.kt` | `isDateInRange` KDoc |
| `javers-core/.../CdoExtensions.kt` | `getWrappedOrNull` KDoc |
| `javers-persistence-redis/.../RedissonCdoSnapshotRepository.kt` | 클래스 KDoc, 2개 property KDoc, 인라인 주석 |

## 번역 기준 적용

- 섹션 헤더: `## 동작/계약` → `## Behavior / Contract`
- `~한다` 서술형 → 3인칭 단수 (`Returns`, `Filters`, `Maps` 등)
- `true/false 반환` → `Returns true when ... / Returns false otherwise`
- 예제 주석 내 한국어 설명 영어화

## 향후 지침

- 새 public API 추가 시 처음부터 영어 KDoc 작성 (CLAUDE.md 정책)
- `## Behavior / Contract` 섹션을 contract-bearing API에 표준으로 적용
