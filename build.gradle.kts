plugins {
    kotlin("jvm") version "2.4.0"
    application
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
    implementation("org.jetbrains.kotlinx:dataframe-excel:1.0.0-rc01")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}