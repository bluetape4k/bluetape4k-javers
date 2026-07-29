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
    implementation(libs.javers.core)
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.benchmark.runtime.jvm)
    implementation(libs.jmh.core)

    implementation(platform(bt4k.exposed.bom))
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation(bt4k.hikaricp)
    implementation(libs.testcontainers.postgresql)
    // 중앙 catalog가 아직 hibernate-envers를 노출하지 않으므로 benchmark 전용 pin으로 좁게 유지합니다.
    implementation("org.hibernate.orm:hibernate-envers:7.3.4.Final")
    runtimeOnly(bt4k.postgresql)
}
