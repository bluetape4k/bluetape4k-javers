configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.bluetape4k.io)
    api(libs.bluetape4k.protobuf)
    api(libs.bluetape4k.jackson3)
    api(libs.bluetape4k.idgenerators)
    compileOnly(libs.bluetape4k.hibernate)
    compileOnly(libs.bluetape4k.cache.core)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.testcontainers)

    // Javers
    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))

    // Redis
    compileOnly(libs.bluetape4k.lettuce)
    compileOnly(libs.bluetape4k.redisson)
    compileOnly(libs.lettuce.core)
    compileOnly(libs.redisson)

    // Codec
    compileOnly(libs.fory.kotlin)
    compileOnly(libs.kryo5)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)
}
