package no.nav.dagpenger.saksbehandling.generell

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.GenerellOppgaveDataDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository

internal fun Route.generellOppgaveApi(oppgaveRepository: OppgaveRepository) {
    route("generell-oppgave") {
        authenticate("azureAd") {
            route("{oppgaveId}") {
                get {
                    val oppgaveId = call.finnUUID("oppgaveId")
                    val data = oppgaveRepository.hentGenerellOppgaveData(oppgaveId)
                    when (data) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else ->
                            call.respond(
                                HttpStatusCode.OK,
                                GenerellOppgaveDataDTO(
                                    emneknagg = data.emneknagg,
                                    tittel = data.tittel,
                                    beskrivelse = data.beskrivelse,
                                ),
                            )
                    }
                }
            }
        }
    }
}
