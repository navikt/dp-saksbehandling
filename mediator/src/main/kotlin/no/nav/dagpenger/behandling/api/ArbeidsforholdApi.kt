package no.nav.dagpenger.behandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.arbeidsforhold.AaregClient

internal fun Application.arbeidsforholdApi(aaregClient: AaregClient) {
    routing {
        authenticate("azureAd") {
            post("arbeidsforhold") {
                val ident = call.receive<Ident>()
                val arbeidsforhold = aaregClient.hentArbeidsforhold(ident.ident, call.request.jwt())
                call.respond(status = HttpStatusCode.OK, arbeidsforhold)
            }
        }
    }
}

internal data class Ident(val ident: String)
