plugins {
    application
    alias(bt4k.plugins.kotlin.serialization)
}

application {
    mainClass.set("io.bluetape4k.javers.examples.ktor.JaversKtorExampleApplicationKt")
}

dependencies {
    implementation(bt4k.bluetape4k.core)
    implementation(bt4k.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform("io.ktor:ktor-bom:${bt4k.versions.ktor.get()}"))
    implementation(platform(bt4k.exposed.bom))
    implementation("io.github.bluetape4k:bluetape4k-ktor-core")
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation("io.ktor:ktor-server-cio")

    runtimeOnly(bt4k.h2.v2)
    runtimeOnly(bt4k.logback)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation("io.github.bluetape4k:bluetape4k-ktor-testing")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(bt4k.h2.v2)
}
