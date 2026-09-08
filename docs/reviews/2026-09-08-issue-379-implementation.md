# #379 구현 검토

## 결과

불완전한 Kafka projection의 offset 확정을 막는다. 승인된 develop 대상 독립 PR 범위로 구현했다.

- 코드 검토: native code-reviewer application_review, P0=0/P1=0/P2=0.
- 설계 검토: native architect adapter_spec, CLEAR.
- 검토 범위: source, 호출 경로, 회귀 테스트, API/ABI와 README. 리뷰 lane은 별도 빌드를 실행하지 않았다.
- 검증: RED: 기존 snapshot의 metadata 누락을 재현한 1개 테스트 실패. GREEN: core 215, Kafka 49, Redis 88개 통과(실패/skip 0). ABI 갱신과 검사 분리 실행 PASS.
- 잔여 계약: metadata가 누락된 기존 row는 새 sequence를 추측하지 않고 복원용 전체 재생을 요구한다. custom metadata 저장소는 검증 hook을 재정의해야 한다.
- 교훈: ../lessons/2026-09-08-issue-379-projection-completeness.md

## DoD

범위·구현·회귀·독립 검토 완료. PR CI와 exact-head 최종 리뷰는 생성 후 검증하며,
7개 PR 전체가 준비될 때까지 병합하지 않는다. 새 의존성·배포·schema 변경은 없다.
