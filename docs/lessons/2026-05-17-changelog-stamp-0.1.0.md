# 교훈: CHANGELOG의 [Unreleased]를 [0.1.0]으로 확정 (이슈 #31)

**날짜**: 2026-05-17
**관련 이슈**: [#31](https://github.com/bluetape4k/bluetape4k-javers/issues/31)

## 변경 내용

`CHANGELOG.md`의 `## [Unreleased]` 헤더를 `## [0.1.0] - 2026-05-17`로 교체.

## 릴리즈 전 완료 체크리스트 (0.1.0)

- [x] #29 bluetape4k 1.8.0-SNAPSHOT → 1.8.0 (PR #51)
- [x] #30 KDoc 영어 번역 (PR #52)
- [x] #31 CHANGELOG 스탬프 (이 PR)
- [ ] #32 Maven Central 비밀 값 확인 (다음 작업)

## 향후 지침

- 릴리스할 때 `[Unreleased]` → `[x.y.z] - YYYY-MM-DD` 패턴을 사용한다.
- Keep a Changelog 형식을 유지한다.
- 스탬프 후 새 `## [Unreleased]` 섹션 추가는 다음 개발 사이클 시작 시 별도 PR로.
