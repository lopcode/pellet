rootProject.name = "pellet"

include("server")
include("logging")
include("demo")
include("bom")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", "1.9.22")
            version("ktor", "2.3.1")
            version("jooby", "2.16.1")
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:5.10.3")
            library("kotlin-serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            library("slf4j-api", "org.slf4j:slf4j-api:2.0.12")
            library("kotlinx-io", "org.jetbrains.kotlinx:kotlinx-io-core:0.3.2")
            library("kotlinx-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            library("okhttp-bom", "com.squareup.okhttp3:okhttp-bom:4.12.0")
            library("ktor-bom", "io.ktor", "ktor-bom").versionRef("ktor")
            library("ktor-core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor-netty", "io.ktor", "ktor-server-netty").versionRef("ktor")
            library("jooby-core", "io.jooby", "jooby").versionRef("jooby")
            library("jooby-netty", "io.jooby", "jooby-netty").versionRef("jooby")
            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("11.4.0")
            plugin("shadow", "com.github.johnrengelman.shadow").version("7.1.0")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
        }
    }
}
