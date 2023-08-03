buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
}

dependencies {
    implementation(project(":modell"))
    implementation(project(":openapi"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.server)

    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.test.host.jvm)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.junit.jupiter.params)
}

repositories {
    mavenCentral()
}

/*application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}*/
