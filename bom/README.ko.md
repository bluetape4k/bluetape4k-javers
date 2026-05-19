# bluetape4k-javers-bom

한국어 | [English](./README.md)

**bluetape4k-javers** 생태계용 Maven BOM (Bill of Materials). 모든 `io.github.bluetape4k.javers:*`
모듈의 버전을 중앙 관리한다.

## Architecture

![Architecture diagram](../docs/images/readme-diagrams/bom-architecture-01.png)

BOM은 Gradle `java-platform` 으로 `<dependencyManagement>` constraint 만 게시한다.

## 핵심 기능

- 모든 `bluetape4k-javers` 모듈 버전 중앙 관리
- Javers audit/diff + Redis/Kafka 백엔드 버전 일관성 보장
- `bluetape4k-dependencies` 가 상위에서 통합

## 관리 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-javers-core` | Javers audit/diff 코어 연동 |
| `bluetape4k-javers-persistence-redis` | Redis 기반 JaversRepository |
| `bluetape4k-javers-persistence-kafka` | Kafka 감사 로그 producer/consumer |

## 사용 예제

### Gradle Kotlin DSL

```kotlin
plugins {
    id("io.spring.dependency-management") version "1.1.x"
}

dependencyManagement {
    imports {
        mavenBom("io.github.bluetape4k.javers:bluetape4k-javers-bom:<version>")
    }
}

dependencies {
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-core")
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-persistence-redis")
}
```

### 순수 Gradle

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k.javers:bluetape4k-javers-bom:<version>"))
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-core")
}
```

### Maven

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.bluetape4k.javers</groupId>
            <artifactId>bluetape4k-javers-bom</artifactId>
            <version>${bluetape4k-javers.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 설정 옵션

BOM 자체는 별도 설정이 없다. SNAPSHOT 사용 시 Sonatype Central Snapshots 저장소 추가:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## 의존성

이 BOM은 `bluetape4k-dependencies` 에서 자동 통합된다. 여러 bluetape4k 생태계를 함께 사용한다면
`io.github.bluetape4k:bluetape4k-dependencies` import 권장.
