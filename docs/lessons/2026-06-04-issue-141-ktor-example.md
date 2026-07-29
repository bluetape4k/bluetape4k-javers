# 이슈 141 Ktor JaVers 예제

## 배경

이슈 #141에는 새로운 JaVers 저장소 기능이 아니라 현재 기능을 보여 주는 Ktor
예제가 필요했다. 이 예제는 `javers-ddd`와 `javers-exposed`를 재사용하고,
Spring에 의존하지 않으며, 프로젝트 이름을 `:examples-javers-*` 접두사 아래에
유지해야 했다.

## 결정

인메모리 H2, Exposed JDBC 및 `ExposedCdoSnapshotRepository`를 사용하는 소규모
실행형 Ktor REST 애플리케이션으로 `:examples-javers-ktor`를 만든다.
상태/준비 상태 확인과 통합 테스트에는 bluetape4k Ktor core/testing 헬퍼를
재사용한다.

## 결과

이 예제는 주문 생성, 결제 완료 표시, 명령 측 상태 조회 및 범위가 제한된 JaVers
스냅샷 이력 조회를 보여 준다. 이제 CI와 Nightly에 이 모듈이 포함되며, README
언어별 파일에는 Ktor/JDBC 블로킹 경계를 설명한다.

## 검증

- `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin :examples-javers-ktor:test :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew projects build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `git diff --check`

## 향후 지침

향후 JaVers 예제에서는 중복이 유지보수 문제가 되기 전까지 공유 예제 도메인을
간접 참조하기보다 범위가 좁은 애플리케이션 로컬 도메인을 우선한다. 게시 대상
제외 로직을 단순하게 유지할 수 있도록 예제 프로젝트 이름은
`:examples-javers-*` 아래에 둔다.
