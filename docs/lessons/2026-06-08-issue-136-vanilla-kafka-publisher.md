# Issue #136 - Vanilla Kafka 스냅샷 발행자

## 배경

`javers-persistence-kafka`에는 이미 Spring Kafka `KafkaTemplate` 기반의 쓰기 전용
저장소가 있었다. Spring을 사용하지 않는 사용자에게는 순수 Apache Kafka 프로듀서
경로가 필요했지만, 재사용 가능한 `bluetape4k-kafka` 헬퍼 아티팩트에는 Spring
Kafka 지원도 포함되어 있었다.

## 결정

`Producer<String, String>`을 직접 받는 `VanillaKafkaCdoSnapshotRepository`를
추가한다. `bluetape4k-kafka` / `bluetape4k-kafka4`는
`javers-persistence-kafka`의 필수 런타임 의존성이 아니라 README 예제에서
프로듀서를 생성할 때 선택적으로 사용하는 헬퍼로 유지한다.

## 결과

이 모듈에는 다음 두 어댑터가 명확히 구분되어 있다.

- Spring Kafka `KafkaTemplate`용 `KafkaCdoSnapshotRepository`.
- Apache Kafka `Producer`용 `VanillaKafkaCdoSnapshotRepository`.

프로듀서 소유권은 기본값이 `false`인 `closeProducerOnClose`로 명시한다.

PR 후 리뷰에서는 구현을 `bluetape4k-code-patterns`에 맞게 강화했다. 저장소는
companion `invoke`로 생성하고, MockK 테스트 협력 객체는
`@BeforeEach clearMocks`로 초기화하는 클래스 필드로 두며, Kafka 테스트 픽스처
ID에는 `Base58.randomString`을 사용한다.

## 검증

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`: PASS, 테스트 18개.
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`: 프로덕션 런타임 검증 결과에는 `kafka-clients`가 있고 `spring-kafka` / `bluetape4k-kafka` 행은 없었다.
- `git diff --check`: PASS.

## 향후 지침

#105 또는 #131에서 읽기 프로젝션이나 복합 영속 이력의 의미 체계를 명시적으로
담당하기 전까지 Kafka 저장소는 쓰기 전용으로 유지한다. Apache Kafka API를 직접
사용해 순수 Kafka 경로를 더 단순하게 유지할 수 있다면 헬퍼 아티팩트를 런타임
의존성으로 추가하지 않는다. 테스트 전용 Kafka 클라이언트와 그룹의 고유 ID에는
UUID에서 파생한 문자열 대신 짧은 Base58 문자열을 사용한다.
