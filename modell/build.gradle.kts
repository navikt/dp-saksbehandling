plugins {
    id("common")
}

repositories {
    mavenCentral()
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.7")
    api(libs.kotlin.logging)
    implementation(libs.jackson.kotlin)
    testImplementation(libs.bundles.kotest.assertions)
}
