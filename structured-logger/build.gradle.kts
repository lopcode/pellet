plugins {
    `java-library`
    kotlin("plugin.serialization") version "1.6.10"
}

group = "dev.pellet.logger"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}
