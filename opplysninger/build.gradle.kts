plugins {
    id("common")
    `java-library`
}
val cucumberVersion = "7.15.0"
dependencies {
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.1")
}
