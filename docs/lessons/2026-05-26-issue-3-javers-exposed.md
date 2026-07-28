# Issue 3 JaVers Exposed 저장소

## 배경

Issue #3에서는 별도의 #77 README 다이어그램 PR과 결합하지 않고 Exposed 기반
JaVers 저장소를 새로 추가해야 했다. 스택 브랜치에 #77 커밋이 잘못 포함된
뒤 `develop`에서 브랜치를 다시 구성했다.

## 결정

`ExposedCdoSnapshotRepository`를 제공하는 독립 모듈로 `javers-exposed`를
추가한다. 인코딩된 각 전체 `CdoSnapshot`을 `javers_snapshot`에 영속화하고
커밋 메타데이터는 `javers_commit`에 유지한다. 저장소 시퀀스는 별도로 저장해서
저장소를 재빌드한 후 `loadHeadId()`가 최신 `CommitId`를 복원할 수 있게 한다.
첫 버전에서는 SQL 쿼리 푸시다운을 제외하고 상속받은 JaVers 필터링 동작을
사용한다.

## 결과

모듈 컴파일과 H2, PostgreSQL, MySQL 저장소 테스트를 통과했다. 이제 CI와
Nightly 워크플로에서 새 모듈을 검증하며 README/BOM 문서에도 새 아티팩트를
표시한다.

## 검증

- `./gradlew :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-exposed:cleanTest :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## 향후 작업

쿼리 양이 늘어나면 동일한 저장소 API 내부에 SQL 푸시다운을 추가한다. JaVers
JSON 호환성을 한곳에서 관리할 수 있도록 전체 스냅샷 페이로드를 단일 기준
정보로 유지한다.
