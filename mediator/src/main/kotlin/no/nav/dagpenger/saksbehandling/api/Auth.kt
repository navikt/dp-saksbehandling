package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.dagpenger.saksbehandling.jwt.saksBehandler
import java.util.UUID

interface OppgaveTilgangsKontroll {
    fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean?

    fun feilMelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String
}

class EgenAnsattTilgangsKontroll(
    private val tillatteGrupper: Set<String>,
    private val erEgenAnssattFun: (UUID) -> Boolean?,
) : OppgaveTilgangsKontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        return erEgenAnssattFun(oppgaveId) == true && saksbehandler.grupper.any { it in tillatteGrupper }
    }

    override fun feilMelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til oppgave $oppgaveId"
    }
}

fun Route.oppgaveTilgangsKontroll(
    tilgangsKontroll: Set<OppgaveTilgangsKontroll>,
    block: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Call) {
        val oppgaveId = call.parameters["oppgaveId"]?.let { UUID.fromString(it) }
        val saksbehandler = call.principal<JWTPrincipal>()?.saksBehandler

        when {
            oppgaveId == null || saksbehandler == null -> {
                val feilGrunn =
                    when (oppgaveId) {
                        null -> "Mangelende oppgaveId"
                        else -> "Manglende principal"
                    }
                call.respond(HttpStatusCode.BadRequest, feilGrunn)
                finish()
            }

            else -> {
                val feilendeValidering =
                    tilgangsKontroll.firstOrNull { it.harTilgang(oppgaveId, saksbehandler) == false }
                when (feilendeValidering) {
                    null -> {
                        proceed()
                    }

                    else -> {
                        call.respond(HttpStatusCode.Forbidden, feilendeValidering.feilMelding(oppgaveId, saksbehandler))
                        finish()
                    }
                }
            }
        }
    }
    block()
}
