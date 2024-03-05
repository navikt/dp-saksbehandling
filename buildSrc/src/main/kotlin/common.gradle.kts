import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
    maven("https://www.jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    val ktlintVersion = "1.1.1"
    // https://pinterest.github.io/ktlint/latest/rules/standard/
    val overstyrteKtlintRegler = mapOf(
        "max_line_length" to "off",
        "ktlint_function_signature_body_expression_wrapping" to "default",
        "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "unset"
    )

    kotlin {
        ktlint(ktlintVersion).editorConfigOverride(overstyrteKtlintRegler)
    }

    kotlinGradle {
        ktlint(ktlintVersion).editorConfigOverride(overstyrteKtlintRegler)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("spotlessApply")
}
