plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20250821.204.956fe0")
        }
    }
}

rootProject.name = "dp-saksbehandling"
include("modell")
include("openapi")
include("mediator")
include("streams-consumer")
