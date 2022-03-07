plugins {
    `java-library`
}

group = "dev.pellet.server"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":structured-logger"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}
