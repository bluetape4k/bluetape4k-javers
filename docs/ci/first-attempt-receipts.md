# CI first-attempt receipt

CI와 Nightly의 Gradle 재시도는 최종 green만으로 첫 실패를 숨기지 않는다. 각
명령은 `.github/scripts/run_with_attempt_receipt.py`를 통해 실행되며, 첫 시도와
모든 후속 시도의 종료 코드·분류·로그 경로를 JSON receipt로 남긴다.

## 판정 규칙

| 분류 | 의미 | 재시도 |
|---|---|---|
| `passed` | 첫 시도 성공 | 없음 |
| `infrastructure_retry` | Testcontainers/Docker/네트워크/Gradle daemon 등 외부 실행 환경 신호가 있는 실패 | 제한 횟수 안에서만 |
| `test_failure` | 제품 코드·테스트·컴파일 실패로 분류되는 실패 | 즉시 중단 |
| `passed_after_retry` | infrastructure retry 뒤 성공 | 최종 green이지만 첫 실패 evidence를 확인 |

알 수 없는 실패는 `test_failure`로 보수적으로 처리한다. 재시도는 안정성 신호를
지우는 면죄부가 아니며, receipt와 GitHub Step Summary에 first-attempt 상태와
retry count를 남긴 뒤에만 green으로 끝난다.

## Receipt와 coverage 해석

각 job은 `build/reports/ci-attempts/` 아래 receipt와 attempt log를 생성한다.
benchmark job은 이를 benchmark artifact와 함께 보존한다. JSON receipt 부재·빈
receipt·예상 matrix 불일치는 기존 benchmark validator가 fail-closed한다.

`paths-filter`로 실행되지 않은 job, 조건식으로 skipped 된 job, 또는 해당 모듈의
coverage artifact가 없는 job은 runtime coverage proof가 아니다. CI status의
green은 실행된 job과 명시된 skip 조건을 함께 읽어야 한다.

재시도 횟수와 green 기준은 workflow의 `--max-attempts`와 이 문서의 표가
정본이다. 테스트 실패를 infrastructure로 오분류하지 않도록 새로운 외부 오류
형태를 추가할 때는 helper 테스트와 이 문서를 함께 갱신한다.
