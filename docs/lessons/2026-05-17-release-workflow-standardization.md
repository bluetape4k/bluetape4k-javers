# 릴리스 워크플로 표준화

배경: Central Portal 릴리스 작업은 `bluetape4k-projects`의 릴리스 워크플로
구성을 표준으로 사용한다.

결정: 워크플로 표시 이름은 그대로 유지하면서 릴리스 준비 워크플로 파일의
이름을 `nightly-tests.yml`과 `publish-snapshot.yml`로 변경한다.

결과: 릴리스 준비 스크립트가 모든 bluetape4k 저장소에서 동일한 워크플로
파일 이름을 사용할 수 있게 되었다.

검증: `actionlint .github/workflows/nightly-tests.yml .github/workflows/publish-snapshot.yml .github/workflows/release.yml`.

향후 준수 사항: 저장소별 예외가 `AGENTS.md`에 문서화되어 있지 않다면 릴리스
워크플로 파일 이름을 `bluetape4k-projects`와 일치시킨다.
