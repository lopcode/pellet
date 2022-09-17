// Without these suppressions version catalog usage here and in other build
// files is marked red by IntelliJ:
// https://youtrack.jetbrains.com/issue/KTIJ-19369.
@Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
    "FUNCTION_CALL_EXPECTED"
)
plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.serialization)
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

    implementation(libs.kotlin.serialization)
    implementation(libs.ktor.core)
    implementation(libs.ktor.netty)
    implementation(libs.jooby.core)
    implementation(libs.jooby.netty)
}

application {
    mainClass.set("dev.pellet.demo.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms1024m", "-Xmx2048m")
}
