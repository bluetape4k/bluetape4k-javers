configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()

dependencies {
    api(bt4k.bluetape4k.core)
    api(bt4k.bluetape4k.jackson3)
    api(bt4k.bluetape4k.idgenerators)

    implementation(platform(bt4kLibrary("bluetape4k-exposed-bom")))
    api(bt4kLibrary("bluetape4k-exposed-jdbc"))

    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))

    api(platform(bt4k.exposed.bom))
    api(bt4k.exposed.core)
    api(libs.exposed.dao)
    api(bt4k.exposed.jdbc)
    api(bt4k.exposed.java.time)

    testImplementation(bt4k.bluetape4k.junit5)
    testImplementation(bt4kLibrary("bluetape4k-exposed-jdbc-tests"))
    testRuntimeOnly(libs.h2)
    testRuntimeOnly(bt4k.postgresql)
    testRuntimeOnly(bt4k.mysql.connector.j)
}
