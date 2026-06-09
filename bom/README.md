# bluetape4k-javers-bom

[한국어](./README.ko.md) | English

Maven BOM (Bill of Materials) for the **bluetape4k-javers** ecosystem. Import it
when an application uses more than one published JaVers artifact and should keep
`bluetape4k-javers-core`, `bluetape4k-javers-ddd`, Exposed, Redis, and Kafka
artifacts on the same release line.

## Architecture

![bom Architecture diagram](../docs/images/readme-diagrams/bom-architecture-01.png)

The BOM is a Gradle `java-platform` that publishes only dependency-management
constraints for deployable Maven artifacts. It adds no runtime classes and does
not publish runnable modules, ecosystem-level metadata, auto-configuration, or
persistence topology decisions.

## Core Features

- Centralized version management for published `bluetape4k-javers` artifacts
- Aligned JaVers audit/diff stack across core, DDD, Exposed, Redis, and Kafka artifact IDs
- No runtime classes, runnable modules, auto-configuration, or persistence behavior
- Aggregated by `bluetape4k-dependencies` for cross-ecosystem version coordination

## Published Artifacts Managed

| Artifact | Description |
|--------|-------------|
| `bluetape4k-javers-core` | Javers audit/diff core integration |
| `bluetape4k-javers-ddd` | DDD aggregate and domain-event helper layer |
| `bluetape4k-javers-exposed` | Exposed JDBC-backed JaversRepository |
| `bluetape4k-javers-persistence-redis` | Redis-backed JaversRepository |
| `bluetape4k-javers-persistence-kafka` | Kafka snapshot event delivery for projection pipelines |
| `bluetape4k-javers-spring-boot4-autoconfigure` | Spring Boot 4 conditional auto-configuration for JaVers repositories |

## Usage

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
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-ddd")
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-exposed")
    implementation("io.github.bluetape4k.javers:bluetape4k-javers-persistence-redis")
}
```

### Plain Gradle

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

## Configuration Options

The BOM itself has no configuration. For SNAPSHOT builds, add the Sonatype Central Snapshots repository:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "central-snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## Dependency

This BOM is automatically aggregated by `bluetape4k-dependencies`. Prefer importing
`io.github.bluetape4k:bluetape4k-dependencies` when consuming multiple bluetape4k ecosystems.
