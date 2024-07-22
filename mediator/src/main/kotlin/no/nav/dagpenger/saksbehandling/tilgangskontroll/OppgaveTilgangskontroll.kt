package no.nav.dagpenger.saksbehandling.tilgangskontroll

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.dagpenger.saksbehandling.api.Saksbehandler
import no.nav.dagpenger.saksbehandling.jwt.saksbehandler
import java.util.UUID

interface OppgaveTilgangskontroll {
    fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean?

    fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String
}

class EgenAnsattTilgangskontroll(
    private val tillatteGrupper: Set<String>,
    private val erEgenAnsattFun: (UUID) -> Boolean?,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        return when (erEgenAnsattFun(oppgaveId)) {
            true -> saksbehandler.grupper.any { it in tillatteGrupper }
            else -> true
        }
    }

    override fun feilmelding(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String {
        return "${saksbehandler.navIdent} har ikke tilgang til oppgave $oppgaveId"
    }
}

class IngenTilgangTilOppgaveException(message: String) : RuntimeException(message)

fun Route.oppgaveTilgangsKontroll(
    tilgangskontroll: Set<OppgaveTilgangskontroll>,
    block: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Call) {
        val oppgaveId = call.parameters["oppgaveId"]?.let { UUID.fromString(it) }
        val saksbehandler = call.principal<JWTPrincipal>()?.saksbehandler

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
                    tilgangskontroll.firstOrNull { it.harTilgang(oppgaveId, saksbehandler) == false }
                when (feilendeValidering) {
                    null -> {
                        proceed()
                    }

                    else -> {
                        throw IngenTilgangTilOppgaveException(feilendeValidering.feilmelding(oppgaveId, saksbehandler))
                    }
                }
            }
        }
    }
    block()
}
