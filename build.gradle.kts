import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    kotlin("jvm") version "2.4.0"
    application
    id("com.gradleup.shadow") version "9.6.1" // can use id("io.ktor.plugin") version "3.5.1" or older instead
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

tasks.named<ShadowJar>("shadowJar") {
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    mergeServiceFiles()
}