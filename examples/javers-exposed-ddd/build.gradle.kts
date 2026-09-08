dependencies {
    implementation(bt4k.bluetape4k.core)
    implementation(bt4k.bluetape4k.lettuce)
    implementation(bt4k.javers.core)
    implementation(project(":javers-ddd"))
    implementation(project(":javers-exposed"))

    implementation(platform(bt4k.exposed.bom))
    implementation(bt4k.exposed.core)
    implementation(bt4k.exposed.jdbc)
    implementation(bt4k.exposed.java.time)
    implementation(libs.kafka.clients)

    runtimeOnly(bt4k.h2.v2)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)
    testImplementation(bt4k.h2.v2)
    testImplementation(bt4k.hikaricp)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(bt4k.postgresql)
}

// 배포하지 않는 공통 행동 계약이며 각 예제의 실제 도메인 구현에 실행합니다.
kotlin.sourceSets.named("test") {
    kotlin.srcDir(rootProject.file("examples/shared-test-contracts"))
}
