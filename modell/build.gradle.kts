plugins {
    id("dagpenger.common")
}

repositories {
    mavenCentral()
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.6")
    api(Kotlin.Logging.kotlinLogging)
}
