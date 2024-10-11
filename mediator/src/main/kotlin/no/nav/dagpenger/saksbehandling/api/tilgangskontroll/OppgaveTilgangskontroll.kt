package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Saksbehandler
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

    fun feilType(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): String
}

class IngenTilgangTilOppgaveException(message: String, val type: String) : RuntimeException(message)
