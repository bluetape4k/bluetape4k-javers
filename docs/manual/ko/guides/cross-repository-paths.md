# 다른 저장소로 이어지는 길

공통 계약을 다른 저장소가 맡는 지점에서 이 매뉴얼의 설명도 멈춥니다.

## Exposed 경계

`javers-exposed`는 JaVers CDO snapshot과 커밋 metadata를 저장합니다. JDBC/R2DBC 애플리케이션 저장소, 트랜잭션 helper, 캐시 쓰기 방식, Ktor helper, Spring 자동 구성은 `bluetape4k-exposed`의 책임입니다. [Exposed 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/)과 [트랜잭션 소유권](https://bluetape4k.github.io/ko/manual/bluetape4k-exposed/1.11/modules/bluetape4k-exposed-jdbc/transaction-ownership/)을 참고하세요.

## Projects 경계

Redis client, Kafka utility, JaVers 흐름 밖의 DDD 기반, Testcontainers launcher는 `bluetape4k-projects`가 맡습니다. [Projects 매뉴얼](https://bluetape4k.github.io/ko/manual/bluetape4k-projects/)에서 확인할 수 있습니다. 0.2.1 예제가 사용하는 기반 의존성은 [`examples/javers-exposed-ddd/build.gradle.kts`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/build.gradle.kts)에 고정돼 있습니다.

## 버전 경계

소비자는 `io.github.bluetape4k:bluetape4k-dependencies` 생태계 버전 하나를 가져와 Projects, Exposed, Redis, Kafka, Javers 버전을 맞춥니다. 애플리케이션 build에서 별도 호환표를 만들지 않는 것이 기본입니다.

Ktor 연동, Spring Boot 4 자동 구성, `examples/javers-ktor`, `examples/javers-spring-boot4`, `benchmark/javers-exposed-benchmark`는 0.2.1 뒤에 추가됐습니다. develop 소스를 0.2 계약으로 읽으면 안 됩니다. 릴리스 모듈은 [`settings.gradle.kts`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/settings.gradle.kts)에서 확인할 수 있습니다.
