# Issue 104 Spring Boot 4 Auto-Configuration 설계

## 맥락

Issue #104는 기존 JaVers repository backend용 Spring Boot 4 auto-configuration module을 요구한다. Repository에는 이미 Exposed, Redis, Kafka, Spring Boot 4 example module이 있지만 재사용 가능한 auto-configuration artifact는 없다.

## 설계

- `javers-spring-boot4-autoconfigure`를 library module로 추가한다.
- Backend infrastructure는 application-owned로 유지한다.
  - Exposed `Database`
  - Lettuce `RedisClient`
  - Redisson `RedissonClient`
  - Spring Kafka `KafkaTemplate<String, String>`
  - vanilla Kafka `Producer<String, String>`
- `bluetape4k.javers.repository.type`이 concrete backend를 선택할 때만 `JaversRepository`를 생성한다.
- Repository가 존재하고 application이 아직 `Javers`를 제공하지 않았을 때만 default `Javers` bean을 생성한다.
- Ordering annotation이 적용되도록 각 backend를 별도 `@AutoConfiguration` class로 `AutoConfiguration.imports`에 직접 등록한다.
- Optional backend와 codec class는 `@ConditionalOnClass(name = [...])`로 guard한다.
- Safe Redis codec choice인 `lz4-fory`만 노출하고 auto-configuration에서 JDK serialization은 노출하지 않는다.

## Non-goal

- DataSource, Redis, Kafka client bean을 생성하지 않는다.
- `examples/javers-spring-boot4`의 explicit wiring을 대체하지 않는다.
- 기존 module의 repository runtime behavior를 변경하지 않는다.
- Hard Kover threshold를 추가하지 않는다.

## 인수 Check

- `repository.type=none`은 repository를 생성하지 않는다.
- Global disable은 repository 또는 `Javers`를 생성하지 않는다.
- 각 supported backend는 required bean이 있을 때 repository를 등록한다.
- User-provided `JaversRepository` 및 `Javers` bean을 존중한다.
- Missing optional backend class는 startup을 실패시키지 않는다.
- `AutoConfiguration.imports`는 모든 phase를 직접 나열한다.
- Root 및 module README locale set은 dependency, property, caller-owned infrastructure를 문서화한다.
- CI와 Nightly는 새 module용 path filter, test job, Kover XML artifact, status/coverage aggregation을 포함한다.

## 7-Tier Spec 검토

| Tier | 결과 | 비고 |
|---|---|---|
| Security | PASS | Secret이나 unsafe deserialization을 노출하지 않는다. Redis JDK codec은 option이 아니다. |
| Ops/SRE | PASS | Startup은 conditional bean/class를 통해 back off한다. Infrastructure ownership은 명시적으로 유지된다. |
| Structural | PASS | 새 module은 optional boundary를 통해 기존 repository module에 의존한다. |
| Kotlin | PASS | Configuration property는 value type이고 public API에는 English KDoc이 있다. |
| Tests | PASS | Slice test는 backend registration, backoff, missing class를 cover한다. |
| Performance | PASS | Hot path나 background lifecycle을 추가하지 않는다. |
| Docs/Release | PASS | README locale set, CHANGELOG, AGENTS, CI/Nightly가 required deliverable이다. |

최종 gate: `P0 = 0`, `P1 = 0`.
