plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "dev.pellet"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":server"))
    implementation(project(":structured-logger"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("io.ktor:ktor-server-core:1.6.7")
    implementation("io.ktor:ktor-server-netty:1.6.7")

    implementation("io.jooby:jooby:2.13.0")
    implementation("io.jooby:jooby-netty:2.13.0")
}

application {
    mainClass.set("dev.pellet.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms512m", "-Xmx1024m")
}
