configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    create("testJar")
}

val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()

tasks.register<Jar>("testJar") {
    dependsOn(tasks.testClasses)
    archiveClassifier.set("test")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testJar", tasks["testJar"])
}

dependencies {
    api(bt4k.javers.core)

    api(bt4kLibrary("bluetape4k-annotations"))
    api(bt4k.bluetape4k.io)
    api(bt4k.bluetape4k.jackson3)
    implementation(bt4k.bluetape4k.cache.core)
    implementation(bt4k.bluetape4k.protobuf)
    implementation(bt4k.bluetape4k.hibernate)
    implementation(bt4k.bluetape4k.idgenerators)
    implementation(bt4k.bluetape4k.redisson)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.bluetape4k.testcontainers)
    testImplementation(bt4k.guava)

    // JaVers repository용 cache
    compileOnly(bt4k.caffeine)
    compileOnly(bt4k.caffeine.jcache)
    compileOnly(bt4k.cache2k.core)

    // Mongo 연동
    compileOnly(bt4k.mongo.bson)
    compileOnly(bt4k.mongo.bson.kotlin)
    compileOnly(bt4k.mongo.bson.kotlinx)
    compileOnly(bt4k.mongodb.driver.sync)
    compileOnly(bt4k.mongodb.driver.kotlin.sync)
    compileOnly(bt4k.mongodb.driver.kotlin.coroutine)

    // Codec 연동
    compileOnly(bt4k.kryo5)
    compileOnly(bt4k.fory.kotlin)

    // 압축
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.zstd.jni)
}
