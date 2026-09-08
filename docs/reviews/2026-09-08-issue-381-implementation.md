# #381 구현 검토

## 결과

여러 이벤트의 개별 감사 metadata를 보존한다. 승인된 develop 대상 독립 PR 범위로 구현했다.

- 코드 검토: native code-reviewer application_review, P0=0/P1=0/P2=0.
- 설계 검토: native architect adapter_spec, CLEAR.
- 검토 범위: source, 호출 경로, 회귀 테스트, API/ABI와 README. 리뷰 lane은 별도 빌드를 실행하지 않았다.
- 검증: RED: 다중 metadata 누락 재현 실패. GREEN: ddd 26개 통과, ABI 검사 PASS.
- 잔여 계약: metadata 수는 이벤트 수에 비례하며 backend 크기 제한은 호출자가 소유한다.
- 교훈: ../lessons/2026-09-08-issue-381-event-cardinality.md

## DoD

범위·구현·회귀·독립 검토 완료. PR CI와 exact-head 최종 리뷰는 생성 후 검증하며,
7개 PR 전체가 준비될 때까지 병합하지 않는다. 새 의존성·배포·schema 변경은 없다.
