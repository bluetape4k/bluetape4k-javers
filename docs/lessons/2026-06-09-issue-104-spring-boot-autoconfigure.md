# 이슈 104 Spring Boot 자동 구성

## 배경

JaVers 저장소의 Spring Boot 자동 구성 작업은 코드, 문서, 모듈 등록,
워크플로 검증 범위에 걸쳐 있었다.

## 결정

모든 인프라 클라이언트의 소유권은 애플리케이션에 두고, 각 저장소 백엔드를
클래스 이름 검사 조건이 있는 직접 등록 방식의 자동 구성 단계로 만든다.

## 결과

새 모듈은 Exposed, Lettuce, Redisson, Spring Kafka, vanilla Kafka 저장소
백엔드가 명시적으로 선택되고 필요한 빈/클래스 경계가 존재할 때만 등록한다.

PR 이후 독립 검토에서 Spring Kafka 자동 구성은 Spring Boot 4 Kafka 자동 구성
뒤에 실행되어야 하고, `kafka.topic`을 Spring Kafka 저장소 경로에 적용해야 하며,
Exposed 스키마 생성은 기본적으로 명시적 활성화 방식이어야 한다는 점을 확인했다.
같은 검토에서 기존 Kafka 저장소/발행자 생성자에 토픽 지원을 추가할 때 발생하는
공개 API 호환성 위험도 발견했다.

## 검증

- Boot가 생성한 `KafkaTemplate`, 구성된 토픽, DDL 기본값 검증을 추가한 뒤
  `./gradlew :javers-spring-boot4-autoconfigure:test ...`에서 테스트 17개가
  통과했다.
- 명시적 토픽 발행자 검증을 추가한 뒤
  `./gradlew :javers-persistence-kafka:test ...`에서 테스트 41개가 통과했다.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`이 통과했다.
- `./gradlew build -x test ...`가 통과했다.

## 향후 준수 사항

새 Spring Boot 자동 구성 모듈을 추가할 때는 `AutoConfiguration.imports`,
README 언어별 파일, CHANGELOG, `AGENTS.md`, CI, Nightly, Kover 산출물을 같은
PR에서 갱신한다. 컴파일 전용 빈 시그니처는
`@ConditionalOnClass(name = [...])`로 보호한다.
독립적인 7-Tier 작업에 하위 에이전트가 필요하지만 호출 도구에 OMX
`agent_type` 필드가 없다면, 해당 작업을 생략하지 말고 역할과 범위를 명시한
하위 에이전트 프롬프트나 OMX 팀 런타임을 사용한다. Boot 4 통합에서는 실제
Boot 모듈을 기준으로 모듈형 자동 구성 클래스 이름과 실행 순서를 검증한다.
공개 Kotlin 생성자나 companion 객체의 팩터리를 확장할 때는 기존 매개변수 순서와 JVM
디스크립터를 유지한다. 새 옵션에는 명시적 오버로드나 이름 있는 팩터리를
추가한 뒤 컴파일 테스트나 `javap`으로 검증한다.
