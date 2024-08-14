package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
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

class IngenTilgangTilOppgaveException(message: String) : RuntimeException(message)

suspend fun PipelineContext<*, ApplicationCall>.oppgaveTilgangskontroll(tilgangskontroll: Set<OppgaveTilgangskontroll>) {
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
            tilgangskontroll.firstOrNull { oppgaveTilgangskontroll ->
                oppgaveTilgangskontroll.harTilgang(oppgaveId, saksbehandler) == false
            }
        when (feilendeValidering) {
            null -> {
                proceed()
            }

            else -> {
                logger.info {
                    "Saksbehandler ${saksbehandler.navIdent} har IKKE tilgang til oppgave med id $oppgaveId. Tilganger: ${saksbehandler.grupper}"
                }
                throw IngenTilgangTilOppgaveException(feilendeValidering.feilmelding(oppgaveId, saksbehandler))
            }
        }
    }
}
