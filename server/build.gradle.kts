
import java.net.URI

plugins {
    `java-library`
    jacoco
    id("maven-publish")
    id("signing")
}

group = "dev.pellet"
val projectTitle = "pellet-server"
project.setProperty("archivesBaseName", projectTitle)
val environmentVersion = System.getenv("VERSION")
if (!environmentVersion.isNullOrBlank()) {
    version = environmentVersion.replaceFirst("v", "")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":logging"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jboss.xnio:xnio-api:3.8.7.Final")
    implementation("org.jboss.xnio:xnio-nio:3.8.7.Final")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

jacoco {
    toolVersion = "0.8.8"
}

val publishingUser = System.getenv("PUBLISHING_USER")
val publishingPassword = System.getenv("PUBLISHING_PASSWORD")
val publishingUrl = System.getenv("PUBLISHING_URL") ?: ""

publishing {
    repositories {
        maven {
            url = URI.create(publishingUrl)
            credentials {
                username = publishingUser
                password = publishingPassword
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "pellet-server"
            from(components["java"])
            pom {
                name.set("Pellet Server")
                description.set("An opinionated, Kotlin-first web framework that helps you write fast, concise, and correct backend services 🚀.")
                url.set("https://github.com/CarrotCodes/Pellet")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://github.com/CarrotCodes/Pellet/blob/main/LICENSE.txt")
                    }
                }
                developers {
                    developer {
                        id.set("carrotcodes")
                        name.set("Carrot")
                    }
                }
                scm {
                    url.set("https://github.com/CarrotCodes/Pellet.git")
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

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}
