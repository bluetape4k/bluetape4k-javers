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

rootProject.name = "bluetape4k-javers"

include(
    "javers-core",
    "javers-persistence-kafka",
    "javers-persistence-redis",
)

include("bluetape4k-javers-bom")
project(":bluetape4k-javers-bom").projectDir = file("bom")
