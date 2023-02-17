buildscript { repositories { mavenCentral() } }

plugins {
    id("dagpenger.common")
    id("dagpenger.rapid-and-rivers")
}

dependencies {
    implementation(project(":modell"))
    implementation("io.ktor:ktor-server-default-headers-jvm:2.1.3")

    testImplementation(Mockk.mockk)

    // demo
    implementation("io.ktor:ktor-server-html-builder:${Ktor2.version}")
    testImplementation(Ktor2.Server.library("test-host"))
}

application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}
