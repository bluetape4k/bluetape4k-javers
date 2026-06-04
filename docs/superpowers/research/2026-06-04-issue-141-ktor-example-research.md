# Issue 141 Ktor JaVers Example Research

## 범위

Issue #141은 현재 `bluetape4k-javers`가 이미 제공하는 기능만 사용하여 Ktor
예제 애플리케이션을 추가하는 작업이다. Spring Boot auto-configuration,
vanilla Kafka adapter, Redis near-cache, pipeline adapter 같은 future feature는
이번 예제의 선행 조건이 아니다.

## 확인한 요구사항

- 새 예제 모듈은 `examples/javers-ktor` 아래에 둔다.
- Gradle project name은 `:examples-javers-ktor`를 사용한다. Issue #140에서
  정한 `examples-javers-*` prefix 규칙을 따라 future publishing exclusion을
  project name 기준으로도 가능하게 한다.
- `javers-exposed`의 `ExposedCdoSnapshotRepository`를 durable JaVers snapshot
  repository로 사용한다.
- `javers-ddd`의 aggregate repository 패턴을 command-side persistence와 JaVers
  commit 흐름에 사용한다.
- Ktor route는 주문 생성, 결제 처리, 현재 상태 조회, audit history 조회를
  제공한다.
- Redis projection, Kafka publish, NATS/SQS pipeline은 현재 이슈 범위 밖이다.

## 공식 Ktor 근거

Context7에서 Ktor 공식 문서 `/ktorio/ktor-documentation`을 확인했다.

- JSON content negotiation에는 `io.ktor:ktor-server-content-negotiation`과
  `io.ktor:ktor-serialization-kotlinx-json`가 필요하다.
- 서버 route는 `Application` module 안에서 `routing { get/post { ... } }`
  구조로 등록할 수 있다.
- 테스트는 `testApplication`과 `createClient { install(ContentNegotiation) {
  json() } }` 조합으로 JSON request/response를 검증할 수 있다.

## bluetape4k Ktor 재사용 근거

`bluetape4k-projects`의 `examples/ktor/idgenerator-ktor-demo`와 Ktor helper
모듈을 확인했다.

- `bluetape4k-ktor-core`
  - `installBluetape4kKtorCore()`가 JSON content negotiation, default error
    responses, `/healthz`, `/readyz`를 설치한다.
  - `requiredPathParameter()`와 `intQueryParameter()`가 path/query validation에
    맞다.
- `bluetape4k-ktor-testing`
  - `bluetape4kJsonClient()`가 Ktor test client JSON 설정을 재사용한다.
  - `decodeJsonBody()`와 `shouldHaveStatus()`를 사용할 수 있다.
- `bluetape4k-ktor-observability`는 유용하지만 이번 예제의 핵심 acceptance가
  JaVers audit wiring이므로 필수 범위에서는 제외한다.

## 의존성 결정

- `bluetape4k-javers`의 local catalog에는 Kotlin serialization plugin alias가
  이미 있다.
- 중앙 `bt4k` catalog에는 `ktor-bom`과 `bluetape4k-ktor-core`,
  `bluetape4k-ktor-testing` alias가 있다.
- 중앙 catalog에는 `ktor-server-core`, `ktor-server-cio`,
  `ktor-server-test-host`, `ktor-server-content-negotiation`,
  `ktor-serialization-kotlinx-json` 개별 alias가 없다.
- 따라서 새 예제 module build script는:
  - `implementation(platform(bt4k.ktor.bom))`
  - `implementation(bt4k.bluetape4k.ktor.core)`
  - `testImplementation(bt4k.bluetape4k.ktor.testing)`
  - Ktor 개별 artifact는 version 없이 좁은 문자열 좌표를 사용한다.

컴파일 단계에서 accessor 이름이 다르면 중앙 catalog accessor를 확인하고 최소
범위에서 조정한다. version duplication은 추가하지 않는다.

## 기존 예제 재사용 결정

- 도메인, command handler, Exposed repository, schema initializer 흐름은
  `examples/javers-spring-boot4`에서 복사하되 package를
  `io.bluetape4k.javers.examples.ktor`로 분리한다.
- Spring MVC controller/validation annotation은 재사용하지 않는다. Ktor route
  DTO는 `kotlinx.serialization.Serializable`을 사용하고 route layer에서 command
  validation을 명시한다.
- `ExposedCdoSnapshotRepository`, `CommitTable`, `CdoSnapshotTable`,
  `OrdersTable` schema initialization은 application module setup에서 수행한다.

## 구현 영향

- 새 모듈: `examples/javers-ktor`
- Gradle project: `:examples-javers-ktor`
- Package: `io.bluetape4k.javers.examples.ktor`
- 문서: module `README.md`, `README.ko.md`, root README locale set, repo
  `AGENTS.md`
- CI/Nightly: path filter, test job, status dependency, Nightly coverage job,
  coverage artifact, aggregation dependency

## 리스크와 대응

| Risk | 대응 |
|---|---|
| Ktor/BOM accessor mismatch | `compileKotlin`로 즉시 검증하고 중앙 catalog alias만 조정한다. |
| JDBC blocking이 Ktor event loop를 막는 오해 | README에 예제는 H2/JDBC audit wiring proof이며 production Ktor에서는 worker dispatcher 또는 virtual-thread runtime strategy를 검토해야 한다고 명시한다. |
| Ktor route validation이 Spring Bean Validation보다 약해질 위험 | DTO to command 변환에서 non-blank, positive, positive amount, history limit cap을 명시하고 테스트한다. |
| JSON serialization에서 `BigDecimal`/`Instant` serializer 누락 | 문자열/epoch가 아닌 Ktor JSON 기본으로 컴파일/테스트 가능한 DTO shape를 선택하거나 custom serializer 없이 동작하는 타입만 노출한다. |
| CI workflow parse 오류 | `actionlint`와 escaped single quote scan을 실행한다. |
| 새 예제가 coverage aggregation에서 누락될 위험 | Nightly coverage artifact와 `needs`를 함께 갱신한다. |
