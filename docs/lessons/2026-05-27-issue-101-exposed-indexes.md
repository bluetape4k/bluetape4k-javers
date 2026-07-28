# 이슈 101 Exposed 인덱스

## 배경

`javers-exposed`는 `global_id`를 기준으로 `version` 순서에 따라 스냅샷 이력을
불러오고, 가장 높은 커밋 시퀀스를 선택해 저장소의 최신 상태를 복원한다.

## 결정

기존의 고유 `(global_id, version)` 인덱스를 스냅샷 이력의 주요 경로 인덱스로
유지하고, 테스트에서 사용할 수 있도록 이름을 스키마 상수로 공개한다. 최신 상태
복원을 위해 `javers_commit`에는 명시적인 `sequence` 인덱스를 추가한다.

## 결과

저장소별 시나리오는 이제 H2 전용 검증에만 의존하지 않고 공통 Exposed JDBC
방언 매트릭스에서 실행된다.

## 검증

`./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
명령으로 저장소 시나리오를 H2, PostgreSQL, MySQL_V8 매트릭스에서 검증했고
모두 통과했다.
