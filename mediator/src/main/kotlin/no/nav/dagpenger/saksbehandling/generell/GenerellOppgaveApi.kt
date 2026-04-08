package no.nav.dagpenger.saksbehandling.generell

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.GenerellOppgaveDataDTO
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveDataRepository

internal fun Route.generellOppgaveApi(generellOppgaveDataRepository: GenerellOppgaveDataRepository) {
    route("generell-oppgave-data") {
        authenticate("azureAd") {
            route("{oppgaveId}") {
                get {
                    val oppgaveId = call.finnUUID("oppgaveId")
                    val data = generellOppgaveDataRepository.hent(oppgaveId)
                    when (data) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else ->
                            call.respond(
                                HttpStatusCode.OK,
                                GenerellOppgaveDataDTO(
                                    oppgaveType = data.oppgaveType,
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
