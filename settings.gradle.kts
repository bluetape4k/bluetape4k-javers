pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    }
}

val bluetape4kDependenciesVersion = providers.gradleProperty("bluetape4kDependenciesVersion").get()

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
    versionCatalogs {
        create("bt4k") {
            from("io.github.bluetape4k:bluetape4k-version-catalog:$bluetape4kDependenciesVersion")
        }
    }
}

rootProject.name = "bluetape4k-javers"

include(
    "javers-core",
    "javers-persistence-kafka",
    "javers-persistence-redis",
)

include("bluetape4k-javers-bom")
project(":bluetape4k-javers-bom").projectDir = file("bom")
