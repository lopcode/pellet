plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "dev.skye.pellet"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("org.slf4j:slf4j-simple:1.7.32")

    implementation("io.ktor:ktor-server-core:1.6.4")
    implementation("io.ktor:ktor-server-netty:1.6.4")

    implementation("io.jooby:jooby:2.11.0")
    implementation("io.jooby:jooby-netty:2.11.0")
}

application {
    mainClass.set("dev.skye.pellet.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms512m", "-Xmx1024m")
}
