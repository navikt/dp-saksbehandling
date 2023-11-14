buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
}

val githubUser: String? by project
val githubPassword: String? by project

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")

    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/dp-kontrakter")
    }
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()
    implementation(project(":modell"))
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

    // Kontrakter for dp-iverksett
    implementation("no.nav.dagpenger.kontrakter:iverksett:2.0_20231114144943_2428cb9")

    testImplementation(libs.mockk)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}

application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}
