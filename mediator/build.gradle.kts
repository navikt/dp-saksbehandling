import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
    id("common")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()
    implementation(project(":openapi"))
    implementation(libs.kotlin.logging)
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation(libs.bundles.postgres)
    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    api("com.fasterxml.uuid:java-uuid-generator:4.3.0")

    testImplementation(libs.mockk)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
application {
    mainClass.set("no.nav.dagpenger.saksbehandling.AppKt")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
