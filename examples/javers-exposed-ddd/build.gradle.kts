dependencies {
    implementation(libs.bluetape4k.core)
    implementation(libs.bluetape4k.lettuce)
    implementation(libs.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform(libs.exposed.bom))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.kafka.clients)

    runtimeOnly(libs.h2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.h2)
    testImplementation(libs.testcontainers.kafka)
}
