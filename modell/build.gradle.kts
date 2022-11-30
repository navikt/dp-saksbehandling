plugins {
    id("dagpenger.common")
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.5")
    api(Kotlin.Logging.kotlinLogging)
    testImplementation(Junit5.params)
}
