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
    implementation("com.fasterxml.uuid:java-uuid-generator:5.0.0")
    implementation("de.slub-dresden:urnlib:2.0.1")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
}
