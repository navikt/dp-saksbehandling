plugins {
    id("common")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.7")
    api(libs.kotlin.logging)
    implementation(libs.jackson.kotlin)
    implementation("no.nav.dagpenger:aktivitetslogg:1.0")
    testImplementation(libs.bundles.kotest.assertions)
}
