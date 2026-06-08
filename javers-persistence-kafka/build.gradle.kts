configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.bluetape4k.core)
    api(libs.bluetape4k.jackson3)
    api(libs.bluetape4k.idgenerators)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.testcontainers)

    // Javers
    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))
    testImplementation(project(":javers-persistence-redis"))

    // Kafka
    implementation(libs.bluetape4k.kafka)
    compileOnly(libs.spring.kafka)

    // Redis projection target tests
    testImplementation(libs.bluetape4k.lettuce)
    testImplementation(libs.lettuce.core)

    // Codec
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)

    // Test
    testImplementation(libs.testcontainers.kafka)
}
