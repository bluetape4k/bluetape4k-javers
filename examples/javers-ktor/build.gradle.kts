plugins {
    application
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("io.bluetape4k.javers.examples.ktor.JaversKtorExampleApplicationKt")
}

dependencies {
    implementation(libs.bluetape4k.core)
    implementation(libs.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform("io.ktor:ktor-bom:${bt4k.versions.ktor.get()}"))
    implementation(platform(libs.exposed.bom))
    implementation("io.github.bluetape4k:bluetape4k-ktor-core")
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation("io.ktor:ktor-server-cio")

    runtimeOnly(libs.h2)
    runtimeOnly(libs.logback)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation("io.github.bluetape4k:bluetape4k-ktor-testing")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(libs.h2)
}
