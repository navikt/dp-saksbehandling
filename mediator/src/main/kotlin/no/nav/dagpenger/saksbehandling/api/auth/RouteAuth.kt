package no.nav.dagpenger.saksbehandling.api.auth

import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

internal fun Route.authenticateSaksbehandlerEllerLes(build: Route.() -> Unit) {
    authenticate(
        strategy = AuthenticationStrategy.FirstSuccessful,
        configurations = arrayOf("azureAd", "azureAdLes"),
    ) {
        build()
    }
}

internal fun Route.authenticateSaksbehandler(build: Route.() -> Unit) {
    authenticate("azureAd") {
        build()
    }
}
