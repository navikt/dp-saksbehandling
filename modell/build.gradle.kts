plugins {
    id("common")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    api("no.nav.dagpenger:aktivitetslogg:20230830.cf9ebc")
    implementation(libs.jackson.kotlin)
    testImplementation(libs.bundles.kotest.assertions)
}
