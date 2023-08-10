buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
}

dependencies {
    implementation(project(":modell"))
    implementation(project(":openapi"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")

    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host.jvm)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mock.oauth2.server)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}
