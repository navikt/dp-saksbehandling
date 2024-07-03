plugins {
    id("common")
}

dependencies {
    api("org.apache.kafka:kafka-streams:3.7.1")
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.server.cio)
    implementation(libs.konfig)

    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.7.1")
    testImplementation(libs.bundles.kotest.assertions)
}
