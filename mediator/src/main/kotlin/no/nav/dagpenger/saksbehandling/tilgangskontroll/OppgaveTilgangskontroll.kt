package no.nav.dagpenger.saksbehandling.tilgangskontroll

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.Saksbehandler
import no.nav.dagpenger.saksbehandling.jwt.saksbehandler
import java.util.UUID

private val logger = KotlinLogging.logger {}

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

class EgneAnsatteTilgangskontroll(
    private val tillatteGrupper: Set<String>,
    private val skjermesSomEgneAnsatteFun: (UUID) -> Boolean?,
) : OppgaveTilgangskontroll {
    override fun harTilgang(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Boolean {
        return when (skjermesSomEgneAnsatteFun(oppgaveId)) {
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
        if (oppgaveId == null) {
            call.respond(HttpStatusCode.BadRequest, "Manglende oppgaveId")
            finish()
        }
        val saksbehandler = call.principal<JWTPrincipal>()?.saksbehandler
        if (saksbehandler == null) {
            call.respond(HttpStatusCode.BadRequest, "Manglende saksbehandler")
            finish()
        }

        if (oppgaveId != null && saksbehandler != null) {
            val feilendeValidering =
                tilgangskontroll.firstOrNull { it.harTilgang(oppgaveId, saksbehandler) == false }
            when (feilendeValidering) {
                null -> {
                    logger.info { "Saksbehandler $saksbehandler har tilgang til oppgave med id $oppgaveId" }
                    proceed()
                }

                else -> {
                    logger.info { "Saksbehandler $saksbehandler har IKKE tilgang til oppgave med id $oppgaveId" }
                    throw IngenTilgangTilOppgaveException(feilendeValidering.feilmelding(oppgaveId, saksbehandler))
                }
            }
        }
    }
    block()
}
