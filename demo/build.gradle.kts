plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("plugin.serialization") version "1.6.21"
}

group = "dev.pellet"

repositories {
    mavenCentral()
}

dependencies {
    // implementation(platform("dev.pellet:pellet-bom:0.0.5"))
    // implementation("dev.pellet:pellet-server")
    // implementation("dev.pellet:pellet-logging")

    // Local development:
    implementation(project(":logging"))
    implementation(project(":server"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("io.ktor:ktor-server-core:1.6.7")
    implementation("io.ktor:ktor-server-netty:1.6.7")

    implementation("io.jooby:jooby:2.13.0")
    implementation("io.jooby:jooby-netty:2.13.0")
}

application {
    mainClass.set("dev.pellet.demo.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms1024m", "-Xmx2048m")
}
