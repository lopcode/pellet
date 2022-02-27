plugins {
    `java-library`
}

group = "dev.pellet.server"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":structured-logger"))
}
