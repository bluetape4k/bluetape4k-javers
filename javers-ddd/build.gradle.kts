configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.core)
    api(bt4k.javers.core)
    api(project(":javers-core"))

    compileOnly(libs.spring.kafka)
    compileOnly(bt4k.bluetape4k.nats)

    testImplementation(project(":javers-exposed"))
    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4k.h2.v2)
    testImplementation(bt4k.mockk)
}
