package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakBrevVariantRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakUtvidetBeskrivelseRequestDTO
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser

internal fun Route.meldingOmVedtakApi(applicationCallParser: ApplicationCallParser) {
    authenticate("azureAd") {
        route("oppgave/{oppgaveId}/melding-om-vedtak") {
            get("html") {
                val oppgaveId = call.finnUUID("oppgaveId")
                val saksbehandler = applicationCallParser.saksbehandler(call)
                // TODO: Hent oppgave, berik med person/saksbehandler-data, kall dp-melding-om-vedtak
                call.respond(HttpStatusCode.OK)
            }

            route("utvidet-beskrivelse/{brevblokkId}") {
                put {
                    val oppgaveId = call.finnUUID("oppgaveId")
                    val brevblokkId =
                        call.parameters["brevblokkId"]
                            ?: throw IllegalArgumentException("Kunne ikke finne brevblokkId i path")
                    val request = call.receive<MeldingOmVedtakUtvidetBeskrivelseRequestDTO>()
                    val saksbehandler = applicationCallParser.saksbehandler(call)
                    // TODO: Slå opp behandlingId fra oppgave, kall dp-melding-om-vedtak
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("brev-variant") {
                put {
                    val oppgaveId = call.finnUUID("oppgaveId")
                    val request = call.receive<MeldingOmVedtakBrevVariantRequestDTO>()
                    val saksbehandler = applicationCallParser.saksbehandler(call)
                    // TODO: Slå opp behandlingId fra oppgave, kall dp-melding-om-vedtak
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
