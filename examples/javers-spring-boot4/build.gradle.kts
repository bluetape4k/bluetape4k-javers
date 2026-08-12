plugins {
    alias(bt4k.plugins.kotlin.spring)
}

dependencies {
    implementation(bt4k.bluetape4k.core)
    implementation(bt4k.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform("org.springframework.boot:spring-boot-dependencies:${bt4k.versions.spring.boot4.get()}"))
    implementation(platform(bt4k.exposed.bom))
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    runtimeOnly(bt4k.h2.v2)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)
    testImplementation(bt4k.h2.v2)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly(bt4k.postgresql)
}
