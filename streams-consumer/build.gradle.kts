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

dependencies {
    api("org.apache.kafka:kafka-streams:3.8.0")
    implementation("org.apache.avro:avro:1.12.0")
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.server.cio)
    implementation(libs.konfig)

    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.8.0")
    testImplementation(libs.bundles.kotest.assertions)
}

tasks.named("runKtlintFormatOverTestSourceSet") {
    dependsOn(tasks.named("generateTestAvroJava"))
}

tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn(tasks.named("generateTestAvroJava"))
}
