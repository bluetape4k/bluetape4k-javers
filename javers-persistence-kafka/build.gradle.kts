configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.core)
    api(bt4k.bluetape4k.jackson3)
    api(bt4k.bluetape4k.idgenerators)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)

    // Javers
    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))
    testImplementation(project(":javers-exposed"))
    testImplementation(project(":javers-persistence-redis"))

    // Kafka
    implementation(bt4k.bluetape4k.kafka)
    compileOnly(libs.spring.kafka)

    // Redis projection target tests
    testImplementation(bt4k.bluetape4k.lettuce)
    testImplementation(libs.lettuce.core)

    // Codec
    compileOnly(bt4k.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // Test
    testImplementation(libs.testcontainers.kafka)
    testRuntimeOnly(libs.h2)
}
