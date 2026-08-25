plugins {
    alias(bt4k.plugins.kotlinx.benchmark)
    kotlin("plugin.allopen")
}

val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()

tasks.withType<JavaExec>().configureEach {
    if (name.endsWith("Benchmark")) {
        val reportDirectory = layout.buildDirectory.dir("reports/benchmarks").get().asFile
        jvmArgs("-Dbluetape4k.benchmark.report-directory=$reportDirectory")
    }
}

allOpen {
    annotation("kotlinx.benchmark.State")
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("main") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = bt4k.versions.managed.jmh.core.h350a653f63e5.get()
        }
    }
    configurations {
        named("main") {
            include(".*ExposedCommitMetadataIndexBenchmark.*")
            include(".*EnversComparisonBenchmark.*")
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
        register("enversComparisonSmoke") {
            include(".*EnversComparisonBenchmark.*")
            warmups = 1
            iterations = 1
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "json"
        }
    }
}

dependencies {
    implementation(project(":examples-javers-exposed-ddd"))
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform(bt4kLibrary("bluetape4k-exposed-bom")))
    implementation(bt4kLibrary("bluetape4k-jdbc"))
    implementation(bt4kLibrary("bluetape4k-exposed-jdbc"))
    implementation(bt4kLibrary("bluetape4k-exposed-jdbc-tests"))

    implementation(bt4k.bluetape4k.core)
    implementation(bt4k.bluetape4k.testcontainers)
    implementation(bt4k.javers.core)
    implementation(bt4k.kotlinx.benchmark.runtime)
    implementation(bt4k.kotlinx.benchmark.runtime.jvm)
    implementation(bt4k.jmh.core)

    implementation(platform(bt4k.exposed.bom))
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation(bt4k.hikaricp)
    implementation(libs.testcontainers.postgresql)
    implementation(bt4k.hibernate73.envers)
    runtimeOnly(bt4k.postgresql)
}
