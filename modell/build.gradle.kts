plugins {
    id("common")
    `java-test-fixtures`
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
    implementation("io.prometheus:prometheus-metrics-core:1.5.0")
    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}
