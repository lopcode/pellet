import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
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
        useJUnitPlatform()
        description = "Runs integration tests."
        group = "verification"

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))
        implementation(platform(rootProject.libs.kotlin.coroutines.bom))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
        implementation(rootProject.libs.slf4j.api)

        testImplementation(kotlin("test-junit5"))
        testImplementation(rootProject.libs.junit.jupiter)

        integrationTestImplementation(kotlin("test-junit5"))
        integrationTestImplementation(rootProject.libs.junit.jupiter)
        integrationTestImplementation(platform(rootProject.libs.okhttp.bom))
        integrationTestImplementation("com.squareup.okhttp3:okhttp")
    }

    tasks.test {
        useJUnitPlatform()
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
