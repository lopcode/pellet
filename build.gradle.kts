import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.7.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("maven-publish")
}

repositories {
    mavenCentral()
}

subprojects {
    if (name.endsWith("bom")) {
        return@subprojects
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    java {
        withJavadocJar()
        withSourcesJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    kotlin {
        jvmToolchain {
            this.languageVersion.set(JavaLanguageVersion.of("17"))
        }
    }

    sourceSets.create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
        java.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
    }

    val integrationTestImplementation by configurations.getting {
        extendsFrom(configurations.implementation.get())
    }

    configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

    task<Test>("integrationTest") {
        description = "Runs integration tests."
        group = "verification"

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))
        implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.1"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
        implementation("org.slf4j:slf4j-api:1.7.32")

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))

        integrationTestImplementation(kotlin("test"))
        integrationTestImplementation(kotlin("test-junit"))
        integrationTestImplementation((platform("com.squareup.okhttp3:okhttp-bom:4.9.3")))
        integrationTestImplementation("com.squareup.okhttp3:okhttp")
    }

    tasks.test {
        testLogging {
            events = TestLogEvent.values().toSet() - TestLogEvent.STARTED
            exceptionFormat = TestExceptionFormat.FULL
        }
        exclude("dev.pellet.integration")
    }

    tasks.named<Test>("integrationTest") {
        systemProperty("benchmark.requests.total", System.getProperty("benchmark.requests.total"))
        testLogging {
            events = TestLogEvent.values().toSet() - TestLogEvent.STARTED
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
