# 단일 모듈 Kover artifact layout 실패 교훈

## 상황

`fix/306-lettuce-close`의 코드 검증과 `javers-persistence-redis` Testcontainers
테스트는 통과했지만, PR #314의 `Coverage Report`가 실패했습니다. Redis 테스트 job은
`coverage-persistence-redis` artifact를 성공적으로 업로드했는데, 단일 artifact를
`actions/download-artifact@v8`로 내려받으면 `coverage-artifacts/report.xml`처럼
artifact 이름 디렉터리가 생략될 수 있습니다.

기존 validator는 `coverage-artifacts/coverage-persistence-redis/report.xml`만
확인했기 때문에 정상 report를 누락으로 오인했습니다. `CI Status` 실패는 이
`Coverage Report` 실패의 연쇄 결과였습니다.

## 원인과 수정

- 단일 artifact의 평탄화된 `report.xml`/`reportJvm.xml`을 기대 artifact 디렉터리로
  감쌉니다.
- 단일 artifact가 모듈 디렉터리(`javers-persistence-redis/**`)로 내려온 경우에도
  기대 artifact 아래로 이동합니다.
- 다중 artifact의 기존 이름 있는 디렉터리 layout은 그대로 검증합니다.
- report가 없거나 빈 artifact는 계속 실패시켜 coverage 누락을 숨기지 않습니다.
- CI와 full nightly workflow가 동일한 validator와 회귀 테스트를 사용하도록
  검증 계약을 단일화합니다.

## 검증 원칙

실패 artifact를 `/tmp`에 내려받아 실제 `report.xml` layout을 재현한 뒤, 다음 세
경로를 각각 검증합니다.

1. 단일 artifact가 root에 `report.xml`을 둔 경우
2. 단일 artifact가 모듈 디렉터리를 둔 경우
3. 다중 artifact가 이름 있는 디렉터리를 둔 경우

`bluetape4k-aws#440`의 동일한 문제 해결 패턴을 참고했지만, 이 저장소의
`coverage-persistence-*` 및 `examples-*` artifact 이름을 별도로 고정했습니다.

## 재발 방지

새 모듈이나 Kover artifact 이름을 추가할 때는 upload job, 변경 모듈 filter,
validator mapping, CI/nightly 기대 목록, 회귀 테스트를 함께 갱신합니다. 단일
artifact 성공만으로 다중 artifact layout이 검증됐다고 간주하지 않습니다.
