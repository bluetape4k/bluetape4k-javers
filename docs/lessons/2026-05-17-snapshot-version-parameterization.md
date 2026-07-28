# 스냅샷 버전 매개변수화

배경: Central Portal 릴리스에서 `-SNAPSHOT`을 제거하기 위해
`gradle.properties`를 수정할 필요가 없어야 한다.

결정: `snapshotVersion=`의 기본값을 비워 두고 `publish-snapshot.yml`에서
`-PsnapshotVersion=-SNAPSHOT`을 전달한다.

결과: `develop` 브랜치는 릴리스 준비 상태를 유지하고, 스냅샷 게시는 워크플로
명령에 명시적으로 드러난다.

검증: `actionlint .github/workflows/publish-snapshot.yml`.

향후 준수 사항: `gradle.properties`에 `snapshotVersion=-SNAPSHOT`을 기본값으로
다시 추가하지 않는다.
