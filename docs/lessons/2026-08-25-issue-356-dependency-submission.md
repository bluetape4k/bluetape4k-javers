# Issue #356 Dependency Submission과 Dependency Graph capability 경계

## 상황

Epic #344의 최종 cumulative head `0c91c600e5225df41e847bb5e770bd8d8d3ddfd2`를
`develop`에 병합한 뒤 `Dependency Submission` workflow run
[#32796933167](https://github.com/bluetape4k/bluetape4k-javers/actions/runs/32796933167)이
실패했다. 같은 push의 주 CI run
[#32796933142](https://github.com/bluetape4k/bluetape4k-javers/actions/runs/32796933142)은
17/17 job이 성공했지만, dependency graph 입력 데이터 제출 단계에서 다음 오류가 발생했다.

`The Dependency graph is disabled for this repository. Please enable it before submitting snapshots.`

## 결정 또는 발견 사항

- 현재 repository API에서 `security_and_analysis.dependency_graph.status`가
  `enabled`가 아닌 상태이고, SBOM endpoint도 비활성 capability를 반환한다.
- repository 보안 설정을 이 변경에서 임의로 활성화하지 않고, workflow가 실행 전에
  capability를 확인하도록 한다.
- `enabled` 상태에서만 `gradle/actions/dependency-submission`을 실행한다.
- `disabled` 상태에서는 `notice`와 `GITHUB_STEP_SUMMARY`에 skip 사유와 정책을 남긴다.
- 예상하지 못한 상태는 조용히 건너뛰지 않고 workflow를 실패시켜 설정 drift를 드러낸다.
- `push`와 `workflow_dispatch` 모두 `github.sha`를 checkout해 branch 이름이 아닌
  이벤트의 exact commit을 대상으로 삼는다.

GitHub는 dependency submission이 만든 의존성 입력 데이터를 dependency graph에 반영하고, 제출된
의존성을 dependency review와 Dependabot 보안 기능의 입력으로 사용한다. 따라서
graph capability가 꺼진 상태에서 제출 action만 호출하는 것은 정상적인 실패이며,
workflow가 그 경계를 먼저 기록해야 한다.

## 결과

Dependency Graph가 비활성화된 현재 정책에서는 Gradle 의존성 입력 데이터 생성과 원격 제출을
실행하지 않고, 실행 결과에 의도적인 skip receipt를 남긴다. 관리자가 graph를
활성화하면 동일한 workflow가 `enabled` 경로로 전환되어 의존성 입력 데이터 제출을 재개한다.
활성화 후 제출 action 자체가 실패하면 기존처럼 workflow 실패와 action 로그를
확인할 수 있다.

## 검증

- repository live read-back: `security_and_analysis`와 dependency graph SBOM endpoint를
  확인해 현재 capability가 비활성임을 검증했다.
- `actionlint .github/workflows/dependency-submission.yml`
- workflow YAML/static contract assertions와 `git diff --check`
- hosted exact-head `workflow_dispatch`: disabled notice, step summary, submission
  step skip 여부를 확인한다.

## 향후 guard

Dependency Submission workflow를 추가하거나 변경할 때는 의존성 입력 데이터를 생성하기 전에
repository capability를 먼저 판정한다. `disabled`와 `enabled` 경로를 각각 검증하고,
의존성 입력 데이터 생성 성공과 원격 제출 성공을 하나의 성공 신호로 합치지 않는다. repository
보안 설정을 변경하는 작업은 별도의 권한·보안 검토와 live read-back을 거친 뒤
수행한다.
