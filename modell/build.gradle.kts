plugins {
    id("dagpenger.common")
}

repositories {
    mavenCentral()
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.6")
    api(Kotlin.Logging.kotlinLogging)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}
