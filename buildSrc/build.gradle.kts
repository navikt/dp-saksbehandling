plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.7.21"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}