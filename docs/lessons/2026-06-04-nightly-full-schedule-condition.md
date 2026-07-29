# 교훈 — Nightly 전체 일정 조건 (2026-06-04)

**관련 이슈**: #152

## 배경

이전 Nightly 스냅샷 새로 고침 PR은 병합된 것으로 표시되었지만 현재 `develop`에는
워크플로 변경 사항이 없었다. 후속 확인에서는 전체 범위 예약 작업이 여전히
`github.event.schedule`을 이전 일요일 cron 문자열과 비교하고 있음을 발견했다.

## 결정

현재 `develop`에 Nightly 스냅샷 새로 고침 및 시차 변경을 다시 적용하고, 전체 범위
작업 조건을 저장소의 현재 일요일 일정에 맞춘다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml`
- `git diff --check`
- 일정 조건 감사: 이전 `0 19 * * 0` 전체 작업 조건이 남아 있지 않음.

## 향후 규칙

예약 cron 문자열을 변경할 때는 같은 워크플로의 모든 `github.event.schedule` 비교를
함께 갱신한다. PR이 병합된 것으로 표시되었지만 `develop`이 이동하지 않았다면 로컬
복구 컨텍스트를 삭제하기 전에 브랜치 참조를 확인한다.
