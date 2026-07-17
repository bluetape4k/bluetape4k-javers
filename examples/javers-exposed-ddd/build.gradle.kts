dependencies {
    implementation(bt4k.bluetape4k.core)
    implementation(bt4k.bluetape4k.lettuce)
    implementation(libs.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform(bt4k.exposed.bom))
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation(libs.kafka.clients)

    runtimeOnly(libs.h2)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)
    testImplementation(libs.h2)
    testImplementation(bt4k.hikaricp)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(bt4k.postgresql)
}
