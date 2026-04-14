plugins {
    kotlin("jvm") version "2.2.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.axay:simplekotlinmail-core:1.4.0")
    implementation("net.axay:simplekotlinmail-client:1.4.0")
    implementation("net.axay:simplekotlinmail-html:1.4.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}