
import java.net.URI

// Without these suppressions version catalog usage here and in other build
// files is marked red by IntelliJ:
// https://youtrack.jetbrains.com/issue/KTIJ-19369.
@Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
    "FUNCTION_CALL_EXPECTED"
)
plugins {
    `java-library`
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
}

group = "dev.pellet"
val projectTitle = "pellet-logging"
project.setProperty("archivesBaseName", projectTitle)
val environmentVersion = System.getenv("VERSION")
if (!environmentVersion.isNullOrBlank()) {
    version = environmentVersion.replaceFirst("v", "")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.serialization)
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
            artifactId = "pellet-logging"
            from(components["java"])
            pom {
                name.set("Pellet Logging")
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
                        id.set("carrotcodes")
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

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}
