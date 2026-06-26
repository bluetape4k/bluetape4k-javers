plugins {
    `java-platform`
    `maven-publish`
    signing
}

dependencies {
    constraints {
        val dependencyHandler = project.dependencies
        rootProject.subprojects {
            if (name != "bluetape4k-javers-bom") {
                api(dependencyHandler.project(mapOf("path" to path)))
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("BluetapeJavers") {
            from(components["javaPlatform"])
            pom {
                name.set("bluetape4k-javers-bom")
                description.set("BOM for bluetape4k-javers — Javers audit/diff with DDD helpers, Exposed, Redis, and Kafka backends")
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
}

configurePublishingSigning("BluetapeJavers")
