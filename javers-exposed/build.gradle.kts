configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()

dependencies {
    api(libs.bluetape4k.core)
    api(libs.bluetape4k.jackson3)
    api(libs.bluetape4k.idgenerators)

    implementation(platform(bt4kLibrary("bluetape4k-exposed-bom")))
    api(bt4kLibrary("bluetape4k-exposed-jdbc"))

    api(libs.javers.core)
    api(project(":javers-core"))
    testImplementation(project(path = ":javers-core", configuration = "testJar"))

    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(bt4kLibrary("bluetape4k-exposed-jdbc-tests"))
    testRuntimeOnly(libs.h2)
    testRuntimeOnly(libs.postgresql)
    testRuntimeOnly(libs.mysql.connector.j)
}
