# Issue #136 - Vanilla Kafka Snapshot Publisher 설계

## 목표

Non-Spring application이 Apache Kafka client API로 audit event를 publish할 수 있도록 JaVers CDO snapshot용 Spring-free Kafka publish path를 추가한다.

## 근거

- GitHub issue #136: vanilla Kafka snapshot publisher using bluetape4k-kafka.
- 현재 base: `develop`의 `4e49fa7 docs: clarify Redis Exposed latency strategy`.
- 현재 `KafkaCdoSnapshotRepository`는 `KafkaTemplate<String, String>`을 받고 `sendDefault(...).get(publishTimeout)`으로 publish한다.
- `javers-persistence-kafka/build.gradle.kts`는 `kafka-clients`를 `api`로 노출하고 `spring-kafka`를 `compileOnly`로 유지한다.
- `gradle/libs.versions.toml`은 `bluetape4k-kafka`를 포함한다.
- Sibling `bluetape4k-projects/infra/kafka`는 vanilla `producerOf(...)`와 `Producer.suspendSend(...)` helper를 제공하지만, published Kafka utility artifact도 Spring Kafka support를 포함한다. 따라서 `javers-persistence-kafka`는 Spring-free repository를 노출한다는 이유만으로 `bluetape4k-kafka`를 mandatory runtime dependency로 추가하면 안 된다.
- Prior #40 / `docs/lessons/2026-05-17-javers-0.1.0-prerelease-fixes.md`는 P0 class bug를 기록한다. Kafka publish failure는 propagate되어야 하고 write-only read behavior는 명시적으로 유지되어야 한다.

## 범위

### Public API

- `VanillaKafkaCdoSnapshotRepository`를 추가한다.
  - Constructor는 Apache Kafka `Producer<String, String>`을 받는다.
  - Constructor는 topic, publish timeout, flush-after-send flag, close-producer-on-close flag를 가진 options object를 받는다.
  - Constructor는 default `snapshot.globalId.value()`를 사용하는 optional key mapper를 받는다.
  - Class는 opt-in producer ownership을 명확히 하기 위해 `AutoCloseable`을 구현한다.
- `VanillaKafkaCdoSnapshotRepositoryOptions`를 추가한다.
  - `topic`은 non-blank여야 한다.
  - `publishTimeout`은 positive여야 한다.
  - `flushAfterSend` default는 `false`다. `send(...).get(...)`이 이미 acknowledgement를 기다린다.
  - `closeProducerOnClose` default는 `false`다. Caller가 보통 Apache Kafka producer lifecycle을 소유하기 때문이다.

### Behavior 계약

- Persisted JaVers snapshot마다 `ProducerRecord(topic, key, encodedSnapshot)`를 publish한다.
- Kafka send result를 `publishTimeout`까지 기다린다.
- `AbstractCdoSnapshotRepository.persist()`가 head commit을 advance하지 않도록 publish failure를 `RuntimeException`으로 propagate한다.
- Interrupted되면 interrupt status를 보존한다.
- 기존 Kafka repository와 맞춰 repository를 write-only로 유지한다. Read method는 empty/false/0을 반환하고 write-only contract를 warn level로 한 번 log한 뒤 debug로 log한다.
- 이 issue에서 Kafka를 read-capable하게 만들지 않는다. #105가 read projection을 소유하고 #131이 composite durable history plus event stream behavior를 소유한다.
- Vanilla path용 mandatory Spring Kafka runtime dependency를 추가하지 않는다.

### 문서

- `javers-persistence-kafka/README.md`와 `README.ko.md`를 갱신한다.
- Spring Kafka 및 vanilla Kafka adapter choice를 보여준다.
- Producer creation을 위한 optional `bluetape4k-kafka` usage를 보여주되 이를 `javers-persistence-kafka`의 mandatory dependency로 만들지 않는다.
- Write-only warning을 계속 보이게 둔다.

## 테스트 요구사항

- Unit test:
  - Vanilla repository의 topic, key, encoded payload를 capture한다.
  - Publish failure propagation을 증명한다.
  - Timeout propagation을 증명한다.
  - Interrupted publish가 interrupt status를 restore함을 증명한다.
  - `flushAfterSend`가 successful ack 이후에만 `Producer.flush()`를 호출함을 증명한다.
  - `closeProducerOnClose=false`가 producer를 close하지 않음을 증명한다.
  - `closeProducerOnClose=true`가 `close()` 호출 시 producer를 close함을 증명한다.
  - Blank topic과 non-positive timeout validation을 증명한다.
  - Write-only read contract warning parity를 증명한다.
- 기존 Spring Kafka repository test는 계속 통과해야 한다.
- Targeted Gradle verification:
  - `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Dependency evidence:
  - `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`
  - Production runtime classpath는 `spring-kafka`를 포함하면 안 된다.
- `git diff --check`.

## Non-goal

- Kafka read projection.
- Composite durable history plus event stream repository.
- New module.
- Spring Boot auto-configuration.
- Coroutine repository API.
- Caller가 해당 producer setting을 configure한다는 문서화를 넘어선 Kafka transaction 또는 idempotence default.

## Risk 및 Mitigation

- Risk: `bluetape4k-kafka`를 runtime dependency로 추가하면 Spring Kafka가 transitively 다시 들어올 수 있다. Mitigation: Apache `Producer`를 직접 받고 `bluetape4k-kafka`는 optional producer factory/helper로 문서화한다.
- Risk: Write-only repository가 JaVers repository contract를 구현하므로 read-capable하게 보일 수 있다. Mitigation: 기존 warning behavior와 README warning을 유지한다.
- Risk: Producer lifecycle ownership이 모호하다. Mitigation: default를 `closeProducerOnClose=false`로 두고 사용자가 opt in할 때 명시적 `AutoCloseable` behavior를 제공한다.
- Risk: Blocking `Future.get()`은 timeout 없이는 hang될 수 있다. Mitigation: positive `publishTimeout`을 validate하고 모든 send wait에 사용한다.
