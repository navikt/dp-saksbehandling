import org.jetbrains.kotlin.de.undercouch.gradle.tasks.download.Download

plugins {
    id("org.openapi.generator") version "7.4.0"
    id("de.undercouch.download") version "5.6.0"
    id("common")
    `java-library`
}

tasks {
    compileKotlin {
        dependsOn("openApiGenerate")
    }
    spotlessKotlin {
        dependsOn("openApiGenerate")
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin"))
        }
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
}

val schema = "behandling-api.yaml"

tasks.register<Download>("downloadOpenApi") {
    src("https://raw.githubusercontent.com/navikt/dp-behandling/main/openapi/src/main/resources/$schema")
    dest("$projectDir/src/main/resources/$schema")
    overwrite(false)
}

tasks.named("openApiGenerate") {
    dependsOn("downloadOpenApi")
}

tasks.named("processResources") {
    dependsOn("downloadOpenApi")
}
openApiGenerate {
    generatorName.set(
        "kotlin",
    )
    inputSpec.set("$projectDir/src/main/resources/behandling-api.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.dagpenger.behandling.opplysninger.api")
    globalProperties.set(
        mapOf(
            "apis" to "none",
            "models" to "",
        ),
    )
    modelNameSuffix.set("DTO")
    templateDir.set("$projectDir/src/main/resources/templates")
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "original",
        ),
    )
}
