plugins {
    id("ch.acanda.gradle.fabrikt") version "1.13.0"
    id("common")
    idea
    `java-library`
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerate")
    }
}

tasks.named("runKtlintCheckOverMainSourceSet").configure {
    dependsOn("fabriktGenerate")
}

tasks.named("runKtlintFormatOverMainSourceSet").configure {
    dependsOn("fabriktGenerate")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin"))
        }
    }
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated") }
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
}

fabrikt {
    generate("saksbehandling") {
        apiFile = file("$projectDir/src/main/resources/saksbehandling-api.yaml")
        basePackage = "no.nav.dagpenger.saksbehandling.api"
        skip = false
        quarkusReflectionConfig = disabled
        typeOverrides {
            datetime = LocalDateTime
        }
        model {
            generate = enabled
            validationLibrary = NoValidation
            extensibleEnums = disabled
            sealedInterfacesForOneOf = enabled
            ignoreUnknownProperties = disabled
            nonNullMapValues = enabled
            serializationLibrary = Jackson
            suffix = "DTO"
        }
    }
    generate("oneof") {
        apiFile = file("$projectDir/src/main/resources/oneof.yaml")
        basePackage = "no.nav.dagpenger.saksbehandling.oneof"
        skip = false
        quarkusReflectionConfig = disabled
        typeOverrides {
            datetime = LocalDateTime
        }
        model {
            generate = enabled
            validationLibrary = NoValidation
            extensibleEnums = disabled
            sealedInterfacesForOneOf = enabled
            ignoreUnknownProperties = disabled
            nonNullMapValues = enabled
            serializationLibrary = Jackson
            suffix = "DTO"
        }
    }
}

// openApiGenerate {
//    generatorName.set(
//        "kotlin",
//    )
//    inputSpec.set("$projectDir/src/main/resources/saksbehandling-api.yaml")
//    outputDir.set("${layout.buildDirectory.get()}/generated/")
//    packageName.set("no.nav.dagpenger.saksbehandling.api")
//    globalProperties.set(
//        mapOf(
//            "apis" to "none",
//            "models" to "",
//        ),
//    )
//    modelNameSuffix.set("DTO")
//    templateDir.set("$projectDir/src/main/resources/templates")
//    configOptions.set(
//        mapOf(
//            "serializationLibrary" to "jackson",
//            "enumPropertyNaming" to "original",
//        ),
//    )
//    typeMappings.set(
//        mapOf(
//            "DateTime" to "LocalDateTime",
//        ),
//    )
//
//    importMappings.set(
//        mapOf(
//            "LocalDateTime" to "java.time.LocalDateTime",
//        ),
//    )
// }
