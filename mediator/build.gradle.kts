buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
}

dependencies {
    implementation(project(":modell"))
    implementation(project(":openapi"))
    implementation(libs.kotlin.logging)
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")
    implementation(libs.bundles.postgres)

    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host.jvm)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}
