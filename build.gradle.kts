import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import groovy.json.JsonOutput
import nmcp.NmcpAggregationExtension
import nmcp.NmcpExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.util.concurrent.TimeUnit

plugins {
    base
    `maven-publish`
    signing
    alias(bt4k.plugins.kotlin.jvm)

    alias(bt4k.plugins.kotlin.spring) apply false
    alias(bt4k.plugins.kotlin.allopen) apply false
    alias(bt4k.plugins.kotlin.noarg) apply false
    alias(bt4k.plugins.kotlinx.atomicfu)

    alias(bt4k.plugins.detekt.legacy)
    alias(bt4k.plugins.dependency.management)

    alias(bt4k.plugins.dokka)
    alias(bt4k.plugins.test.logger)

    alias(bt4k.plugins.nmcp.aggregation)
    alias(bt4k.plugins.nmcp) apply false

    alias(bt4k.plugins.kover)
}

val rootLibs = libs
val rootBt4k = bt4k
val bt4kCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("bt4k")
fun bt4kLibrary(alias: String) = bt4kCatalog.findLibrary(alias).get()
fun bt4kVersion(alias: String): String {
    val version = bt4kCatalog.findVersion(alias).get()
    return version.requiredVersion
        .ifBlank { version.preferredVersion }
        .ifBlank { version.strictVersion }
}

fun Project.isExampleProject(): Boolean {
    val relativeProjectDir = projectDir.relativeTo(rootProject.projectDir).invariantSeparatorsPath
    return name.startsWith("examples-javers-") ||
        relativeProjectDir.startsWith("examples/") ||
        name.startsWith("benchmark-") ||
        relativeProjectDir.startsWith("benchmark/")
}

val centralPublishing = resolveCentralPublishingConfig()
val centralUser: String = centralPublishing.username
val centralPassword: String = centralPublishing.password
val centralSnapshotsParallelism: Int = providers
    .gradleProperty("centralSnapshotsParallelism")
    .map(String::toInt)
    .orElse(4)
    .get()

val projectGroup = providers.gradleProperty("projectGroup").get()
val baseVersion = providers.gradleProperty("baseVersion").get()
val snapshotVersion = providers.gradleProperty("snapshotVersion").get()

allprojects {
    group = projectGroup
    version = baseVersion + snapshotVersion

    repositories {
        mavenCentral()
        // bluetape4k SNAPSHOT 버전 사용 시
        maven {
            name = "central-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.DAYS)
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }
    apply(plugin = "com.gradleup.nmcp")

    configurations.matching { it.name.startsWith("nmcp") }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
                useVersion("1.9.0")
                because("nmcp runtime compatibility")
            }
        }
    }

    plugins.withId("com.gradleup.nmcp") {
        if (!isExampleProject()) {
            extensions.configure<NmcpExtension>("nmcp") {
                publishAllPublicationsToCentralPortal {
                    username.set(centralUser)
                    password.set(centralPassword)
                    publishingType.set("AUTOMATIC")
                    uploadSnapshotsParallelism.set(centralSnapshotsParallelism)
                }
            }
        }
    }
}

subprojects {
    // BOM 모듈은 java-platform 플러그인을 사용하므로 Java/Kotlin 설정을 건너뜁니다.
    if (name == "bluetape4k-javers-bom") return@subprojects

    apply {
        plugin<JavaLibraryPlugin>()
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlinx.atomicfu")
        plugin("org.jetbrains.kotlinx.kover")
        plugin("maven-publish")
        plugin("signing")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.dokka")
        plugin("com.adarshr.test-logger")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        kotlin {
            jvmToolchain(25)
            compilerOptions {
                languageVersion.set(KotlinVersion.KOTLIN_2_4)
                apiVersion.set(KotlinVersion.KOTLIN_2_4)
                jvmTarget.set(JvmTarget.JVM_25)
                freeCompilerArgs = listOf(
                    "-Xjsr305=strict",
                    "-jvm-default=enable",
                    "-Xstring-concat=indy",
                    "-Xcontext-parameters",
                    "-Xannotation-default-target=param-property"
                )
                val experimentalAnnotations = listOf(
                    "kotlin.RequiresOptIn",
                    "kotlin.ExperimentalStdlibApi",
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlin.experimental.ExperimentalTypeInference",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.InternalCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview",
                    "kotlinx.coroutines.DelicateCoroutinesApi",
                )
                freeCompilerArgs.addAll(experimentalAnnotations.map { "-opt-in=$it" })
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlinx.atomicfu") {
        atomicfu {
            transformJvm = true
            jvmVariant = "VH"
        }
    }

    tasks {
        abstract class TestMutexService: BuildService<BuildServiceParameters.None>
        abstract class SigningMutexService: BuildService<BuildServiceParameters.None>

        val testMutex = gradle.sharedServices.registerIfAbsent("test-mutex", TestMutexService::class) {
            maxParallelUsages.set(1)
        }
        val signingMutex = gradle.sharedServices.registerIfAbsent("signing-mutex", SigningMutexService::class) {
            maxParallelUsages.set(1)
        }

        compileJava { options.isIncremental = true }
        compileKotlin { compilerOptions { incremental = true } }

        test {
            usesService(testMutex)
            useJUnitPlatform()
            jvmArgs(
                "-Xshare:off",
                "-Xms2M",
                "-Xmx4G",
                "-XX:+UseG1GC",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableDynamicAgentLoading",
                "--enable-preview",
                "-Didea.io.use.nio2=true"
            )
            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true
                events("failed")
            }
        }

        withType<Sign>().configureEach {
            usesService(signingMutex)
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            showFullStackTraces = true
        }

        val reportMerge = register<ReportMergeTask>("reportMerge") {
            output.set(rootProject.layout.buildDirectory.file("reports/detekt/merged.xml"))
        }
        withType<Detekt>().configureEach detekt@{
            finalizedBy(reportMerge)
            reportMerge.configure { input.from(this@detekt.xmlReportFile) }
        }

        jar {
            manifest.attributes["Specification-Title"] = project.name
            manifest.attributes["Specification-Version"] = project.version
            manifest.attributes["Implementation-Title"] = project.name
            manifest.attributes["Implementation-Version"] = project.version
            manifest.attributes["Automatic-Module-Name"] = project.name.replace('-', '.')
            manifest.attributes["Created-By"] =
                "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})"
        }

        dokka {
            dokkaPublications.html {
                outputDirectory.set(layout.buildDirectory.asFile.get().resolve("javadoc"))
            }
            dokkaSourceSets.configureEach {
                includes.from(project.files("README.md"))
            }
        }

        clean {
            doLast {
                delete("./.project")
                delete("./out")
                delete("./bin")
            }
        }
    }

    dependencyManagement {
        setApplyMavenExclusions(false)
        imports {
            mavenBom(bt4kLibrary("bluetape4k-bom").get().toString())
            mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4kVersion("kotlinx-coroutines")}")
            mavenBom("org.jetbrains.kotlin:kotlin-bom:${bt4kVersion("kotlin")}")
            mavenBom(rootBt4k.junit.bom.get().toString())
            mavenBom("org.testcontainers:testcontainers-bom:${bt4kVersion("testcontainers")}")
        }

        dependencies {

            // <central-catalog-local-aliases>

            dependency("com.google.protobuf:protobuf-kotlin:${bt4kVersion("protobuf")}")

            dependency("io.lettuce:lettuce-core:${bt4kVersion("lettuce")}")

            dependency("org.apache.kafka:kafka-clients:${bt4kVersion("kafka4")}")

            dependency("org.awaitility:awaitility-kotlin:${bt4kVersion("awaitility")}")

            dependency("org.jetbrains.exposed:exposed-bom:${bt4kVersion("exposed")}")

            dependency("org.jetbrains.kotlin:kotlin-bom:${bt4kVersion("kotlin")}")

            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${bt4kVersion("kotlinx-coroutines")}")

            dependency("org.slf4j:jcl-over-slf4j:${bt4kVersion("slf4j")}")

            dependency("org.slf4j:jul-to-slf4j:${bt4kVersion("slf4j")}")

            dependency("org.slf4j:log4j-over-slf4j:${bt4kVersion("slf4j")}")

            dependency("org.springframework.kafka:spring-kafka:${bt4kVersion("spring-kafka4")}")

            dependency("org.springframework.kafka:spring-kafka-test:${bt4kVersion("spring-kafka4")}")

            dependency("org.testcontainers:testcontainers-bom:${bt4kVersion("testcontainers")}")

            dependency("org.testcontainers:testcontainers-junit-jupiter:${bt4kVersion("testcontainers")}")

            dependency("org.testcontainers:testcontainers-kafka:${bt4kVersion("testcontainers")}")

            dependency("org.testcontainers:testcontainers-mysql:${bt4kVersion("testcontainers")}")

            dependency("org.testcontainers:testcontainers-postgresql:${bt4kVersion("testcontainers")}")

            // </central-catalog-local-aliases>
            dependency("org.apache.fory:fory-kotlin:${bt4kVersion("fory-kotlin")}")
            dependency("com.google.guava:guava:${bt4kVersion("guava")}")
            dependency("org.redisson:redisson:${bt4kVersion("redisson")}")
            dependency("org.slf4j:slf4j-api:${bt4kVersion("slf4j")}")
            dependency("com.github.luben:zstd-jni:${bt4kVersion("zstd-jni")}")
        }
    }

    dependencies {
        add("api", rootBt4k.jetbrains.annotations)

        add("implementation", rootLibs.kotlin.stdlib)
        add("implementation", rootLibs.kotlin.reflect)
        add("testImplementation", rootLibs.kotlin.test)
        add("testImplementation", rootLibs.kotlin.test.junit5)

        add("implementation", rootLibs.kotlinx.coroutines.core)
        add("implementation", rootBt4k.kotlinx.atomicfu)

        add("api", bt4kLibrary("slf4j-api"))
        add("testImplementation", rootBt4k.logback.asProvider())
        add("testImplementation", rootLibs.jcl.over.slf4j)
        add("testImplementation", rootLibs.jul.to.slf4j)
        add("testImplementation", rootLibs.log4j.over.slf4j)

        add("testImplementation", rootLibs.junit.jupiter)
        add("testRuntimeOnly", rootLibs.junit.platform.engine)

        add("testImplementation", rootLibs.awaitility.kotlin)
        add("testImplementation", rootBt4k.mockk)
    }

    if (!isExampleProject()) {
        publishing {
            publications {
                create<MavenPublication>("BluetapeJavers") {
                    val sourcesJar = tasks.register<Jar>("sourcesJar") {
                        archiveClassifier.set("sources")
                        from(sourceSets["main"].allSource)
                    }
                    val javadocJar = tasks.register<Jar>("javadocJar") {
                        archiveClassifier.set("javadoc")
                        from(layout.buildDirectory.dir("javadoc"))
                    }
                    from(components["java"])
                    artifact(sourcesJar)
                    artifact(javadocJar)

                    pom {
                        name.set(project.name)
                        description.set("Javers auditing & diff toolkit for Kotlin — core, Redis/Kafka persistence, and Exposed integration")
                        url.set("https://github.com/bluetape4k/bluetape4k-javers")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/license/mit/")
                            }
                        }
                        developers {
                            developer {
                                id.set("debop")
                                name.set("Sunghyouk Bae")
                                email.set("sunghyouk.bae@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/bluetape4k/bluetape4k-javers.git")
                            developerConnection.set("scm:git:ssh://github.com/bluetape4k/bluetape4k-javers.git")
                            url.set("https://github.com/bluetape4k/bluetape4k-javers")
                        }
                    }
                }
            }
            repositories {
                mavenCentral()
                maven {
                    name = "central-snapshots"
                    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                }
            }
        }

        configurePublishingSigning("BluetapeJavers")
    }
}

extensions.configure<NmcpAggregationExtension>("nmcpAggregation") {
    centralPortal {
        username.set(centralUser)
        password.set(centralPassword)
        publishingType.set("AUTOMATIC")
        uploadSnapshotsParallelism.set(centralSnapshotsParallelism)
    }
}

val manualModuleInventory = subprojects
    .map { subproject ->
        val sourceDir = rootProject.projectDir.toPath()
            .relativize(subproject.projectDir.toPath())
            .toString()
            .replace(File.separatorChar, '/')
        val kind = when {
            sourceDir == "bom" -> "bom"
            sourceDir.startsWith("examples/") || sourceDir.startsWith("benchmark/") -> "example"
            else -> "library"
        }
        linkedMapOf(
            "gradlePath" to subproject.path,
            "projectName" to subproject.name,
            "sourceDir" to sourceDir,
            "kind" to kind,
        )
    }
    .sortedBy { it.getValue("gradlePath") }

tasks.register("exportManualModuleInventory") {
    group = "documentation"
    description = "Exports the deterministic manual project inventory."
    val outputFile = layout.buildDirectory.file("manual/module-inventory.json")
    val inventoryJson = JsonOutput.prettyPrint(JsonOutput.toJson(manualModuleInventory)) + "\n"
    outputs.file(outputFile)
    inputs.property("manualModuleInventoryJson", inventoryJson)

    doLast {
        val target = outputs.files.singleFile
        target.parentFile.mkdirs()
        target.writeText(inventoryJson)
    }
}

dependencies {
    subprojects
        .filter { !it.isExampleProject() }
        .forEach { add("nmcpAggregation", project(it.path)) }
}

dependencies {
    subprojects.filter { it.name != "bluetape4k-javers-bom" }.forEach { sub -> kover(project(sub.path)) }
}
