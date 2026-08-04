configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.core)
    api(bt4k.bluetape4k.jackson3)
    api(bt4k.bluetape4k.idgenerators)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)

    // JaVers
    api(bt4k.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))
    testImplementation(project(":javers-exposed"))
    testImplementation(project(":javers-persistence-redis"))

    // Kafka
    implementation(bt4k.bluetape4k.kafka)
    compileOnly(libs.spring.kafka)

    // Redis projection target 테스트
    testImplementation(bt4k.bluetape4k.lettuce)
    testImplementation(libs.lettuce.core)

    // Codec 연동
    compileOnly(bt4k.fory.kotlin)
    compileOnly(bt4k.kryo5)

    // 압축
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // 테스트
    testImplementation(libs.testcontainers.kafka)
    testRuntimeOnly(bt4k.h2.v2)
}
