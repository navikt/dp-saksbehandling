plugins {
    id("common")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    api(libs.dp.aktivitetslogg)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlin.logging)
    implementation("de.slub-dresden:urnlib:3.0.0")
    implementation("io.prometheus:prometheus-metrics-core:1.4.1")
    implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
}
