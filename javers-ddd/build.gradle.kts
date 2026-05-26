configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(libs.bluetape4k.core)
    api(libs.javers.core)
    api(project(":javers-core"))

    compileOnly(libs.spring.kafka)
    compileOnly(libs.bluetape4k.nats)

    testImplementation(project(":javers-exposed"))
    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.h2)
    testImplementation(libs.mockk)
}
