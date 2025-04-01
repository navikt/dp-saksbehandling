package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.KlageMediator

fun Route.klageApi(mediator: KlageMediator) {
    route("klage") {
        route("{klageId}") {
            get {
                val klageId = call.finnUUID("klageId")
                val klageDTO = mediator.hentKlage(klageId)
                call.respond(HttpStatusCode.OK, klageDTO)
            }
        }
    }
}
