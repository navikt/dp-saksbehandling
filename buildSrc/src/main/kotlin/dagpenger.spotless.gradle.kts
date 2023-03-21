import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktfmt()
        ktlint("0.48.2")
    }
    kotlinGradle {
        this.target("*.gradle.kts")
        ktlint("0.48.2") // or ktfmt() or prettier()
    }
}
tasks.withType<KotlinCompile>().configureEach {
    dependsOn("spotlessApply")
}
