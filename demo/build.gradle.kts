plugins {
    application
}

group = "dev.skye.pellet"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

application {
    mainClass.set("dev.skye.pellet.DemoKt")
    applicationDefaultJvmArgs = listOf("-Xms512m", "-Xmx1024m")
}
