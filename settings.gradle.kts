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
            version("kotlin", "1.7.10")
            version("ktor", "1.6.7")
            version("jooby", "2.16.1")
            library("kotlin-coroutines-bom", "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4")
            library("kotlin-serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
            library("slf4j-api", "org.slf4j:slf4j-api:1.7.32")
            library("okhttp-bom", "com.squareup.okhttp3:okhttp-bom:4.9.3")
            library("ktor-core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor-netty", "io.ktor", "ktor-server-netty").versionRef("ktor")
            library("jooby-core", "io.jooby", "jooby").versionRef("jooby")
            library("jooby-netty", "io.jooby", "jooby-netty").versionRef("jooby")
            plugin("ktlint", "org.jlleitschuh.gradle.ktlint").version("11.0.0")
            plugin("shadow", "com.github.johnrengelman.shadow").version("7.1.0")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
        }
    }
}
