# Issue 140 Spring Boot 4 JaVers Example Research

## 범위

Issue #140은 현재 `bluetape4k-javers`가 이미 제공하는 기능만 사용하여 Spring
Boot 4 예제를 추가하는 작업이다. 이 research는 구현 전에 다음 판단을 고정한다.

- 기존 `examples/javers-exposed-ddd`의 Exposed + JaVers + DDD aggregate 흐름을
  재사용한다.
- 아직 구현되지 않았거나 별도 backlog에 있는 Redis/Kafka/auto-configuration 개선에
  의존하지 않는다.
- Spring Boot 4 예제는 HTTP 진입점을 제공하되, JaVers 저장소 wiring은 명시적으로
  구성한다.

## 확인한 로컬 근거

- `settings.gradle.kts`는 `../bluetape4k-dependencies/gradle/libs.versions.toml`
  중앙 catalog를 `bt4k` 이름으로 import한다.
- 현재 등록 모듈은 `javers-core`, `javers-ddd`, `javers-exposed`,
  `javers-persistence-kafka`, `javers-persistence-redis`,
  `:examples-javers-exposed-ddd`, BOM이다.
- `examples/javers-exposed-ddd`는 다음 도메인/저장 흐름을 이미 제공한다.
  - `Order` aggregate와 `OrderPlaced`, `OrderMarkedPaid` domain event
  - Exposed `OrdersTable` command persistence
  - `ExposedCdoSnapshotRepository` 기반 JaVers snapshot persistence
  - Kafka publisher와 Redis projection은 별도 CQRS 예제 범위
- CI와 Nightly에는 `javers-exposed-ddd` 전용 path filter, test job, coverage
  artifact가 이미 존재하므로 새 예제도 같은 등록면을 가져야 한다.
- repo-local `AGENTS.md`는 새 모듈 추가 시 README locale set, module list,
  CI/Nightly coverage, coverage artifact를 함께 갱신하라고 요구한다.

## Spring Boot 4 근거

- Spring Boot 공식 문서 확인은 Context7의 `/spring-projects/spring-boot/v4.0.3`
  snapshot으로 수행했다.
- 중앙 dependency catalog의 현재 Spring Boot 4 line은 `spring-boot4 = "4.0.6"`
  이다.
- 공식 테스트 문서상 Spring MVC controller 검증은 `@SpringBootTest`와
  `MockMvc` 조합으로 수행할 수 있다. 구현은 Spring MVC REST 예제에 맞춰
  `@SpringBootTest` + `@AutoConfigureMockMvc` 통합 테스트를 사용한다.
- 의존성 버전은 로컬 catalog에 중복 추가하지 않고 중앙 `bt4k` catalog alias를
  우선 사용한다. Gradle accessor가 맞지 않으면 중앙 catalog alias 이름을 확인한 뒤
  최소 범위의 로컬 alias만 추가하되, version duplication은 피한다.

## 재사용 결정

- 도메인 모델과 command persistence는 `javers-exposed-ddd`에서 복사하되,
  package는 `io.bluetape4k.javers.examples.springboot4`로 분리한다.
- Redis/Kafka projection은 이번 예제에서 제외한다. Issue #140의 목표가
  "현재 기능으로 Spring Boot 4 예제"이므로, HTTP controller와 JaVers history
  조회가 핵심이다.
- `javers-exposed`와 `javers-ddd` API는 production module API 그대로 사용한다.
  새 예제가 production module의 public API를 변경하지 않는다.

## 구현 영향

- 새 모듈: `examples/javers-spring-boot4`
- Gradle project name: `:examples-javers-spring-boot4` so future publish
  filters can exclude example projects by prefix.
- Gradle registration: `settings.gradle.kts`
- 문서: root `README.md`, root `README.ko.md`, repo `AGENTS.md`,
  module `README.md`, module `README.ko.md`
- CI/Nightly: path filter, test job, status/coverage dependency, artifact name
- 테스트: H2 기반 Spring Boot random-port integration test

## 리스크와 대응

| Risk | 대응 |
|---|---|
| Spring Boot 4 Gradle accessor 이름 불일치 | 중앙 catalog alias를 `gradle dependencies`/compile로 검증한다. |
| 예제가 future auto-config처럼 보일 위험 | README와 config class에서 "explicit wiring"을 명시한다. |
| REST history 응답이 과도하게 커질 위험 | request parameter limit을 두고 기본값/상한을 문서화한다. |
| CI workflow parse 오류 | `actionlint`와 escaped single quote scan을 실행한다. |
| 예제 모듈이 coverage aggregation에서 누락될 위험 | Nightly coverage artifact와 root kover aggregation 포함 여부를 확인한다. |
