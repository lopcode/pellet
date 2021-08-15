import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.5.20"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.slf4j:slf4j-api:1.7.32")

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))
    }

    tasks.test {
        testLogging {
            events = TestLogEvent.values().toSet() - TestLogEvent.STARTED
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
