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
    api(libs.javers.core)

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
    compileOnly(bt4k.fory.kotlin)

    // Compression
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(bt4k.zstd.jni)
}
