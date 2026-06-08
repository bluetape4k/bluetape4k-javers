plugins {
    alias(libs.plugins.kotlinx.benchmark)
    kotlin("plugin.allopen")
}

val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()

allOpen {
    annotation("kotlinx.benchmark.State")
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("main") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
    configurations {
        named("main") {
            include(".*ExposedCommitMetadataIndexBenchmark.*")
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "json"
        }
        register("commitMetadataSmoke") {
            include(".*ExposedCommitMetadataIndexBenchmark.*")
            warmups = 1
            iterations = 1
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

dependencies {
    implementation(project(":javers-exposed"))

    implementation(platform(bt4kLibrary("bluetape4k-exposed-bom")))
    implementation(bt4kLibrary("bluetape4k-jdbc"))
    implementation(bt4kLibrary("bluetape4k-exposed-jdbc"))
    implementation(bt4kLibrary("bluetape4k-exposed-jdbc-tests"))

    implementation(libs.bluetape4k.core)
    implementation(libs.bluetape4k.testcontainers)
    implementation(libs.javers.core)
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.benchmark.runtime.jvm)
    implementation(libs.jmh.core)

    implementation(platform(libs.exposed.bom))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.hikaricp)
    implementation(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql)
}
