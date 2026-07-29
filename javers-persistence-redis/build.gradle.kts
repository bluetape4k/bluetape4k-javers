configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.io)
    api(bt4k.bluetape4k.protobuf)
    api(bt4k.bluetape4k.jackson3)
    api(bt4k.bluetape4k.idgenerators)
    compileOnly(bt4k.bluetape4k.hibernate)
    compileOnly(bt4k.bluetape4k.cache.core)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)

    // JaVers
    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))

    // Redis
    compileOnly(bt4k.bluetape4k.lettuce)
    compileOnly(bt4k.bluetape4k.redisson)
    compileOnly(libs.lettuce.core)
    compileOnly(bt4k.redisson)

    // Codec 연동
    compileOnly(bt4k.fory.kotlin)
    compileOnly(libs.kryo5)

    // 압축
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(bt4k.zstd.jni)
}
