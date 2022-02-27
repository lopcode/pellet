plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "dev.pellet"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":server"))
    implementation(project(":structured-logger"))
    implementation("org.slf4j:slf4j-simple:1.7.35")

    implementation("io.ktor:ktor-server-core:1.6.7")
    implementation("io.ktor:ktor-server-netty:1.6.7")

    implementation("io.jooby:jooby:2.13.0")
    implementation("io.jooby:jooby-netty:2.13.0")
}

application {
    mainClass.set("dev.pellet.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms512m", "-Xmx1024m")
}
