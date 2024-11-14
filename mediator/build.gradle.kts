import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("common")
    application
    alias(libs.plugins.shadow.jar)
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()
    val dpBibliotekerVersion = "2024.10.31-15.02.1d4f08a38d24"

    implementation(project(":modell"))
    implementation(project(":openapi"))
    implementation(project(":streams-consumer"))

    implementation(libs.kotlin.logging)
    implementation(libs.rapids.and.rivers)
    implementation("io.prometheus:prometheus-metrics-core:1.3.3")
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation(libs.bundles.postgres)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
    implementation("no.nav.dagpenger:pdl-klient:$dpBibliotekerVersion")
    implementation("no.nav.dagpenger:ktor-client-metrics:$dpBibliotekerVersion")
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging.jvm)
    implementation("de.slub-dresden:urnlib:2.0.1")
    implementation("dev.hsbrysk:caffeine-coroutines:1.2.0")

    api("com.fasterxml.uuid:java-uuid-generator:5.1.0")

    testImplementation(libs.mockk)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
    testImplementation(libs.rapids.and.rivers.test)
}
application {
    mainClass.set("no.nav.dagpenger.saksbehandling.AppKt")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
