rootProject.name = "dp-behandling"
include("modell")
include("openapi")
include("mediator")

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20230919.31.4a1791")
        }
    }
}
