plugins {
    id("dagpenger.common")
}

val cucumberVersjon = "7.10.0"

dependencies {
    api("ch.qos.logback:logback-classic:1.4.5")
    api(Kotlin.Logging.kotlinLogging)
    testImplementation(Junit5.params)
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersjon")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersjon")
    testImplementation("org.junit.platform:junit-platform-suite:1.9.1")

    testImplementation(Jackson.kotlin)
    testImplementation(Jackson.jsr310)
}
