# 이슈 140 Spring Boot 4 예제 교훈

## 배경

이슈 #140에서는 현재의 JaVers + Exposed + DDD 헬퍼 기능을 보여 주는
Spring Boot 4 예제를 추가했다.

## 결정

- 향후 게시 로직에서 프로젝트 이름 접두사로 예제를 제외할 수 있도록 예제 모듈의
  Gradle 프로젝트 이름에 `:examples-javers-*` 패턴을 사용한다.
- 예제 파일 경로는 `examples/` 아래에 두고, 기존의 디렉터리 기반 제외 방식을
  예비 수단으로 유지한다.
- 자동 구성 추가 대신 명시적인 Spring Boot 4 구성을 사용한다.
- Boot 4 MVC 테스트 지원에는
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`를
  사용한다.
- Jackson 2의 `com.fasterxml.jackson.module:jackson-module-kotlin`이 아니라
  Jackson 3의 `tools.jackson.module:jackson-module-kotlin`을 사용한다.

## 결과

- `examples/javers-spring-boot4`를 추가했다.
- 기존 예제 Gradle 프로젝트의 이름을 `:examples-javers-exposed-ddd`로 변경했다.
- 새 Spring Boot 4 예제를 위한 CI/Nightly 작업과 커버리지 산출물을 추가했다.
- 최종 `P0 = 0`, `P1 = 0`을 충족한 7단계 명세, 계획 및 코드 리뷰 산출물을
  추가했다.

## 검증

- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
- `./gradlew :examples-javers-exposed-ddd:test :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :examples-javers-spring-boot4:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `rg -n -F "\\'" .github/workflows`
- `git diff --check`

## 향후 작업 지침

- 이 저장소에 새 예제 모듈을 추가할 때는 `:examples-javers-*` 패턴과 일치하는
  프로젝트 이름을 사용한다.
- Spring Boot 4 MVC 테스트에서는 Boot 4 `webmvc.test` 자동 구성 패키지를
  우선 사용하고 `spring-boot-starter-webmvc-test`를 추가한다.
- 작업에서 벤치마크 수치의 기준선을 명시적으로 다시 설정하지 않는 한 벤치마크
  지표 파일은 변경하지 않는다.
