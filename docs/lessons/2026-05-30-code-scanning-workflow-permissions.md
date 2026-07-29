# Code scanning workflow permissions

## 맥락

GitHub CodeQL이 Nightly, snapshot publish, release workflow에 대해
`actions/missing-workflow-permissions` alert를 보고했다.

## 결정

checkout을 사용하는 workflow에는 명시적인 workflow-level `contents: read`
permission을 선언하고, token이 필요 없는 job은 `permissions: {}`로 override하며,
release를 생성하는 GitHub Release job에만 `contents: write`를 유지한다.

## 결과

workflow token 기본값은 CI, publish, release behavior를 바꾸지 않으면서 alert 대상
job에 대해 least-privilege가 됐다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml .github/workflows/publish-snapshot.yml .github/workflows/release.yml`
- `yq` inspection of workflow and job permissions
- `git diff --check`

## 향후 guard

향후 GitHub Actions를 수정할 때는 먼저 명시적인 workflow-level `permissions` block을
추가한 뒤, write access가 필요한 step이 있을 때만 개별 job 권한을 넓힌다.
