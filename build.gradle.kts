plugins {
    kotlin("jvm") version "1.8.22"
    application
    id("io.ktor.plugin") version "2.3.2"
}

application {
    mainClass.set("io.github.allexnik.MainKt")
}

group = "io.github.allexnik"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:dataframe-excel:0.10.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
