# Javers BOM

`bluetape4k-javers-bom`은 이 저장소가 배포하는 라이브러리 아티팩트 다섯 개의 버전을 맞춥니다. 일반 애플리케이션이라면 `bluetape4k-dependencies` 생태계 버전 하나를 가져오는 편이 낫습니다. 이 BOM은 나머지 bluetape4k 라이브러리와의 호환성을 빌드에서 직접 관리해야 할 때만 선택하세요.

## 배포본 의존성 구조

아래 그림은 `0.2.1` 릴리스에서 가져온 것으로, 이 매뉴얼이 설명하는 버전의 BOM 구성을 보여 줍니다.

[![bluetape4k-javers 0.2.1 BOM 구조](../../assets/readme-diagrams/bom-architecture-01.png)](../../assets/readme-diagrams/bom-architecture-01.svg)

## 좌표와 선택 기준

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:0.2.1"))
    implementation("io.github.bluetape4k.javers:javers-core")
    implementation("io.github.bluetape4k.javers:javers-exposed")
}
```

독립된 빌드에서 Javers 모듈을 둘 이상 사용하지만 `io.github.bluetape4k:bluetape4k-dependencies`를 가져올 수 없다면 이 BOM을 사용합니다. 플랫폼에는 버전 제약만 있고 실행 코드는 없습니다. 릴리스 빌드는 BOM 자신을 제외한 모든 하위 프로젝트를 제약에 넣습니다. 정확한 규칙은 [`bom/build.gradle.kts`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/bom/build.gradle.kts)에 있습니다.

## 맞춰 주는 범위

BOM이 맞추는 배포 모듈은 `javers-core`, `javers-ddd`, `javers-exposed`, `javers-persistence-redis`, `javers-persistence-kafka`입니다. 릴리스 소스는 `examples/javers-exposed-ddd` 제약도 만들지만 이 예제는 Maven에 배포하지 않습니다. 의존성 좌표로 쓰지 말고 실행 가능한 소스로 참고하세요. BOM은 영속 저장 방식을 골라 주거나 Lettuce, Redisson, Spring Kafka, NATS 같은 선택 의존성을 자동으로 넣지 않습니다.

다른 bluetape4k 저장소의 버전도 이 BOM의 범위 밖입니다. Projects, Exposed, Javers, Redis, Kafka 연동을 함께 쓴다면 [시작하기](../getting-started.md)의 생태계 플랫폼을 권합니다.

## 실패와 운영 경계

플랫폼은 Javers 모듈끼리 버전이 어긋나는 실수를 막지만, 사용자가 따로 고른 Exposed, Kafka, Redis, bluetape4k 버전의 호환성까지 증명하지는 않습니다. 의존성 잠금과 빌드 스캔으로 실제 해석 결과를 확인하세요. BOM을 올릴 때는 한 번에 올리고, 사용하는 모듈의 통합 테스트와 선택 클라이언트 의존성을 함께 검증해야 합니다.

## 테스트

```bash
./gradlew :bluetape4k-javers-bom:dependencies
./gradlew :javers-core:test :javers-exposed:test
```

Redis나 Kafka 어댑터를 쓴다면 해당 모듈 테스트도 추가합니다. 저장 방식마다 확인할 내용은 [테스트 안내](../guides/testing.md)에 정리했습니다.

## 하지 않는 일

- `bluetape4k-dependencies`를 대신하지 않습니다.
- 의존성을 구성에 자동으로 추가하지 않습니다.
- Exposed, Redis, Kafka를 같은 용도의 저장소로 만들지 않습니다.
- 스키마 마이그레이션, 토픽 생성, Redis 운영 정책을 제공하지 않습니다.

모듈을 고르기 전 [저장소 구성도](../architecture/repository-map.md)를 먼저 읽어 보세요.
