package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class GenerellOppgave(
    val oppgaveId: UUID,
    val emneknagg: String,
    val tittel: String,
    val beskrivelse: String? = null,
    val strukturertData: JsonNode? = null,
)
