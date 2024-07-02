package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.statistikk.StatistikkTjeneste

internal fun Application.statistikkApi(statistikkTjeneste: StatistikkTjeneste) {
    routing {
        authenticate("azureAd") {
            route("statistikk") {
                get {
                    val statistikk = statistikkTjeneste.hentStatistikk(call.navIdent())
                    call.respond(HttpStatusCode.OK, statistikk)
                }
            }
        }
    }
}
