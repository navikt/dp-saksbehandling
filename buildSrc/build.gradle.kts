plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.19.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.19.0")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
