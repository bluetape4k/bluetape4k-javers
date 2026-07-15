configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(bt4k.bluetape4k.core)
    api(libs.javers.core)
    api(project(":javers-core"))

    compileOnly(libs.spring.kafka)
    compileOnly(bt4k.bluetape4k.nats)

    testImplementation(project(":javers-exposed"))
    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(libs.h2)
    testImplementation(libs.mockk)
}
