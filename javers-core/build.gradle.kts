configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    create("testJar")
}

tasks.register<Jar>("testJar") {
    dependsOn(tasks.testClasses)
    archiveClassifier.set("test")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testJar", tasks["testJar"])
}

dependencies {
    api(libs.javers.core)

    api(libs.bluetape4k.io)
    api(libs.bluetape4k.jackson3)
    implementation(libs.bluetape4k.cache.core)
    implementation(libs.bluetape4k.protobuf)
    implementation(libs.bluetape4k.hibernate)
    implementation(libs.bluetape4k.idgenerators)
    implementation(libs.bluetape4k.redisson)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.guava)

    // Cache for Javers repository
    compileOnly(libs.caffeine)
    compileOnly(libs.caffeine.jcache)
    compileOnly(libs.cache2k.core)

    // Mongo
    compileOnly(libs.mongo.bson)
    compileOnly(libs.mongo.bson.kotlin)
    compileOnly(libs.mongo.bson.kotlinx)
    compileOnly(libs.mongodb.driver.sync)
    compileOnly(libs.mongodb.driver.kotlin.sync)
    compileOnly(libs.mongodb.driver.kotlin.coroutine)

    // Codec
    compileOnly(libs.kryo5)
    compileOnly(libs.fory.kotlin)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)
}
