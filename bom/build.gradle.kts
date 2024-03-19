plugins {
    `java-platform`
    id("maven-publish")
    id("signing")
}

group = "dev.pellet"
val projectTitle = "pellet-bom"
val environmentVersion = System.getenv("VERSION")
if (!environmentVersion.isNullOrBlank()) {
    version = environmentVersion.replaceFirst("v", "")
}

javaPlatform {
    allowDependencies()
}

repositories {
    mavenCentral()
}

val subprojectNames = setOf("server", "logging")
dependencies {
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            if (!subprojectNames.contains(subproject.name)) {
                return@forEach
            }
            api(subproject)
        }
    }
}

val publishingUser = System.getenv("PUBLISHING_USER")
val publishingPassword = System.getenv("PUBLISHING_PASSWORD")
val publishingUrl = System.getenv("PUBLISHING_URL") ?: ""

publishing {
    repositories {
        maven {
            url = java.net.URI.create(publishingUrl)
            credentials {
                username = publishingUser
                password = publishingPassword
            }
        }
    }
    publications {
        create<MavenPublication>("mavenBom") {
            artifactId = "pellet-bom"
            from(components["javaPlatform"])
            pom {
                name.set("Pellet BOM")
                description.set("An opinionated, Kotlin-first web framework that helps you write fast, concise, and correct backend services 🚀.")
                url.set("https://github.com/lopcode/Pellet")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://github.com/lopcode/Pellet/blob/main/LICENSE.txt")
                    }
                }
                developers {
                    developer {
                        id.set("lopcode")
                        name.set("Carrot")
                    }
                }
                scm {
                    url.set("https://github.com/lopcode/Pellet.git")
                }
            }
        }
    }
}

val signingKey = System.getenv("SIGNING_KEY_ID")
val signingKeyPassphrase = System.getenv("SIGNING_KEY_PASSPHRASE")

if (signingKey != null && signingKey != "") {
    project.ext["signing.gnupg.keyName"] = signingKey
    project.ext["signing.gnupg.passphrase"] = signingKeyPassphrase
    project.ext["signing.gnupg.executable"] = "/usr/local/bin/gpg"

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}