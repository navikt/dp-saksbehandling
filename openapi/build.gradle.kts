plugins {
    id("org.openapi.generator") version "7.0.1"
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
            setSrcDirs(listOf("$buildDir/generated/src/main/kotlin"))
        }
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
}

openApiGenerate {
    generatorName.set("kotlin") // Egentlig en client generator, men kotlin-server støtter ikke arv i klassene som genereres: https://github.com/OpenAPITools/openapi-generator/issues/11552
    inputSpec.set("$projectDir/src/main/resources/behandling-api.yaml")
    outputDir.set("$buildDir/generated/")
    packageName.set("no.nav.dagpenger.behandling.api")
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
