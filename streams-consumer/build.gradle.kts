plugins {
    id("common")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

repositories {
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
}

avro {
    fieldVisibility.set("PRIVATE")
    stringType.set("String")
    outputCharacterEncoding.set("UTF-8")
}

val kafkaVersion = "4.1.1"
val confluentVersion = "7.9.0"

dependencies {
    api("org.apache.kafka:kafka-streams:$kafkaVersion")
    // api("org.apache.kafka:kafka-streams:3.8.0")
    implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    implementation("org.apache.avro:avro:1.12.1")
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.server.cio)

    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation(libs.bundles.kotest.assertions)
}

tasks.named("runKtlintFormatOverTestSourceSet") {
    dependsOn(tasks.named("generateTestAvroJava"))
}

tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn(tasks.named("generateTestAvroJava"))
}
