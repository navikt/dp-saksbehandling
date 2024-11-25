plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20241125.106.f5f8a9")
        }
    }
}

rootProject.name = "dp-saksbehandling"
include("modell")
include("openapi")
include("mediator")
include("streams-consumer")
