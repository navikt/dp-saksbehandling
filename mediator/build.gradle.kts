buildscript { repositories { mavenCentral() } }

plugins {
    id("dagpenger.common")
    id("dagpenger.rapid-and-rivers")
}

dependencies {
    implementation(project(":modell"))
    implementation("io.ktor:ktor-server-content-negotiation:${Ktor2.version}")
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")
    implementation("io.ktor:ktor-server-call-logging:${Ktor2.version}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.1.3")

    testImplementation(Mockk.mockk)

    // demo
    testImplementation(Ktor2.Server.library("test-host"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.1.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.10")
}
repositories {
    mavenCentral()
}

application {
    mainClass.set("no.nav.dagpenger.behandling.AppKt")
}
